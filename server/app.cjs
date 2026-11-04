const express = require('express');
const cors = require('cors');
const crypto = require('crypto');

function createApp({admin, db, env = process.env}) {
const app = express();
app.use(cors());
app.use(express.json());
async function verifyFirebaseRequest(req, res) {
  const authorization = req.headers.authorization || '';
  const match = /^Bearer\s+(.+)$/i.exec(authorization);
  if (!match) {
    res.status(401).json({ error: 'A Firebase ID token is required' });
    return null;
  }
  try {
    return await admin.auth().verifyIdToken(match[1]);
  } catch {
    res.status(401).json({ error: 'The Firebase ID token is invalid or expired' });
    return null;
  }
}

// Persistent per-account quota works across server instances. Failed attempts count too.
require('./quiz.cjs').registerQuiz(app, {
  express, verify: verifyFirebaseRequest, env,
  reserve: async (uid) => {
    const day = new Date().toISOString().slice(0, 10);
    const ref = db.collection('quizUsage').doc(uid);
    return db.runTransaction(async tx => {
      const snap = await tx.get(ref);
      const used = snap.exists && snap.get('day') === day ? Number(snap.get('count')) || 0 : 0;
      if (used >= 10) return false;
      tx.set(ref, { day, count: used + 1 });
      return true;
    });
  }
});

// Health check - useful for the demo video and for uptime pings
app.get('/', (req, res) => {
  res.json({ service: 'StudyHub API', status: 'ok', time: new Date().toISOString() });
});

// Register an FCM token for the authenticated user's own device.
app.post('/api/notifications/register', async (req, res) => {
  const decoded = await verifyFirebaseRequest(req, res);
  if (!decoded) return;
  const token = typeof req.body?.token === 'string' ? req.body.token.trim() : '';
  const platform = req.body?.platform === 'ios' ? 'ios' : 'android';
  if (token.length < 32 || token.length > 4096) {
    return res.status(400).json({ error: 'A valid Firebase Messaging token is required' });
  }
  try {
    const tokenId = crypto.createHash('sha256').update(token).digest('hex');
    await db.collection('users').doc(decoded.uid).collection('deviceTokens').doc(tokenId).set({
      token,
      platform,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });
    res.status(204).end();
  } catch (err) {
    console.error('push token registration failed', err);
    res.status(500).json({ error: 'Could not register this device for notifications' });
  }
});

// Remove this device token from the authenticated account (for example on sign-out).
app.post('/api/notifications/unregister', async (req, res) => {
  const decoded = await verifyFirebaseRequest(req, res);
  if (!decoded) return;
  const token = typeof req.body?.token === 'string' ? req.body.token.trim() : '';
  if (token.length < 32 || token.length > 4096) {
    return res.status(400).json({ error: 'A valid Firebase Messaging token is required' });
  }
  try {
    const tokenId = crypto.createHash('sha256').update(token).digest('hex');
    await db.collection('users').doc(decoded.uid).collection('deviceTokens').doc(tokenId).delete();
    res.status(204).end();
  } catch (err) {
    console.error('push token unregistration failed', err);
    res.status(500).json({ error: 'Could not unregister this device' });
  }
});

// Send a data-only push after confirming the message was actually written by the caller.
app.post('/api/notifications/group-message', async (req, res) => {
  const decoded = await verifyFirebaseRequest(req, res);
  if (!decoded) return;
  const groupId = typeof req.body?.groupId === 'string' ? req.body.groupId.trim() : '';
  const messageId = typeof req.body?.messageId === 'string' ? req.body.messageId.trim() : '';
  if (!validResourceId(groupId) || !validResourceId(messageId)) {
    return res.status(400).json({ error: 'groupId and messageId are required' });
  }

  try {
    const groupRef = db.collection('groups').doc(groupId);
    const [groupSnapshot, messageSnapshot] = await Promise.all([
      groupRef.get(),
      groupRef.collection('messages').doc(messageId).get()
    ]);
    if (!groupSnapshot.exists || !messageSnapshot.exists) {
      return res.status(404).json({ error: 'Group or message not found' });
    }
    const group = groupSnapshot.data() || {};
    const message = messageSnapshot.data() || {};
    const memberIds = Array.isArray(group.memberIds) ? group.memberIds : [];
    if (!memberIds.includes(decoded.uid) || message.senderId !== decoded.uid) {
      return res.status(403).json({ error: 'You cannot notify this group about that message' });
    }

    const recipientIds = [...new Set(memberIds.filter((uid) => typeof uid === 'string' && uid !== decoded.uid))];
    const deviceSnapshots = await Promise.all(recipientIds.map((uid) =>
      db.collection('users').doc(uid).collection('deviceTokens').get()
    ));
    const byToken = new Map();
    deviceSnapshots.forEach((snapshot, index) => {
      const userId = recipientIds[index];
      snapshot.docs.forEach((device) => {
        const token = device.get('token');
        if (typeof token === 'string' && token.length > 0) {
          byToken.set(`${userId}:${token}`, { token, ref: device.ref, userId });
        }
      });
    });
    const targets = [...byToken.values()];
    if (targets.length === 0) return res.status(204).end();

    const invalidTokenRefs = [];
    const senderName = String(message.senderName || 'StudyHub member').slice(0, 80);
    const messageText = String(message.text || '').slice(0, 240);
    for (let offset = 0; offset < targets.length; offset += 500) {
      const batch = targets.slice(offset, offset + 500);
      const result = await admin.messaging().sendEach(batch.map((target) => ({
        token: target.token,
        data: {
          type: 'group_message',
          groupId,
          recipientUid: target.userId,
          senderName,
          messageText
        },
        android: { priority: 'high' }
      })));
      result.responses.forEach((response, index) => {
        if (!response.success && [
          'messaging/invalid-registration-token',
          'messaging/registration-token-not-registered'
        ].includes(response.error?.code)) {
          invalidTokenRefs.push(batch[index].ref.delete());
        }
      });
    }
    if (invalidTokenRefs.length > 0) await Promise.all(invalidTokenRefs);
    res.status(204).end();
  } catch (err) {
    console.error('group message push failed', err);
    res.status(500).json({ error: 'Could not send group message notifications' });
  }
});

// GET /api/quote/today?lang=en|af|zu - stable daily quote with localized fallbacks
app.get('/api/quote/today', async (req, res) => {
  const requestedLanguage = String(req.query.lang || 'en').toLowerCase().split('-')[0];
  const language = ['en', 'af', 'zu'].includes(requestedLanguage) ? requestedLanguage : 'en';
  const localQuotes = {
    en: 'Small consistent effort beats one long night before the deadline.',
    af: 'Klein, volgehoue pogings is beter as een lang nag voor die sperdatum.',
    zu: 'Umzamo omncane oqhubekayo ungcono kunokufunda ubusuku bonke ngaphambi komnqamulajuqu.'
  };
  try {
    const snapshot = await db.collection('quotes').get();
    if (snapshot.empty) {
      return res.json({ id: 'default', content: localQuotes[language], author: 'StudyHub', language });
    }
    const docs = snapshot.docs;
    // Same quote for everyone speaking a given language on a given day.
    const dayIndex = Math.floor(Date.now() / 86400000) % docs.length;
    const doc = docs[dayIndex];
    const data = doc.data();
    const translated = data.translations?.[language] || {};
    const content = translated.content || data[`content_${language}`] ||
      (language === 'en' ? data.content : localQuotes[language]);
    const author = translated.author || data[`author_${language}`] || data.author || 'StudyHub';
    res.json({ id: doc.id, content, author, language });
  } catch (err) {
    console.error('quote failed', err);
    res.status(500).json({ error: 'Could not load the quote of the day' });
  }
});

function parseResourceInput(body) {
  if (!body || typeof body !== 'object' || Array.isArray(body)) return null;
  const title = typeof body.title === 'string' ? body.title.trim() : '';
  const subject = typeof body.subject === 'string' ? body.subject.trim() : '';
  const url = typeof body.url === 'string' ? body.url.trim() : '';
  if (!title || title.length > 160 || subject.length > 100 || !url || url.length > 2048) return null;
  try {
    const parsed = new URL(url);
    if (!['http:', 'https:'].includes(parsed.protocol) || !parsed.hostname || parsed.username || parsed.password) {
      return null;
    }
  } catch {
    return null;
  }
  return { title, subject, url };
}

function validResourceId(id) {
  return typeof id === 'string' && /^[A-Za-z0-9_-]{1,200}$/.test(id);
}

// GET /api/resources?subject=Databases
app.get('/api/resources', async (req, res) => {
  try {
    const subject = typeof req.query.subject === 'string' ? req.query.subject.trim() : '';
    if (subject.length > 100) return res.status(400).json({ error: 'subject is too long' });
    let query = db.collection('resources');
    if (subject) query = query.where('subject', '==', subject);
    const snapshot = await query.limit(50).get();
    res.json(snapshot.docs.map((d) => ({ id: d.id, ...d.data() })));
  } catch (err) {
    console.error('resources failed', err);
    res.status(500).json({ error: 'Could not load resources' });
  }
});

// POST /api/resources - every contribution is tied to the authenticated author.
app.post('/api/resources', async (req, res) => {
  const decoded = await verifyFirebaseRequest(req, res);
  if (!decoded) return;
  const resource = parseResourceInput(req.body);
  if (!resource) return res.status(400).json({ error: 'Provide a title, subject, and valid HTTP(S) URL' });
  try {
    const ref = await db.collection('resources').add({
      ...resource,
      addedBy: decoded.uid,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
    res.status(201).json({ id: ref.id, ...resource, addedBy: decoded.uid });
  } catch (err) {
    console.error('create resource failed', err);
    res.status(500).json({ error: 'Could not save the resource' });
  }
});

// PUT /api/resources/:id - authors may edit only their own resource.
app.put('/api/resources/:id', async (req, res) => {
  const decoded = await verifyFirebaseRequest(req, res);
  if (!decoded) return;
  if (!validResourceId(req.params.id)) return res.status(400).json({ error: 'Invalid resource id' });
  const resource = parseResourceInput(req.body);
  if (!resource) return res.status(400).json({ error: 'Provide a title, subject, and valid HTTP(S) URL' });
  try {
    const ref = db.collection('resources').doc(req.params.id);
    const snapshot = await ref.get();
    if (!snapshot.exists) return res.status(404).json({ error: 'Resource not found' });
    if (snapshot.get('addedBy') !== decoded.uid) return res.status(403).json({ error: 'You cannot edit this resource' });
    await ref.update(resource);
    res.json({ id: req.params.id, ...resource, addedBy: decoded.uid });
  } catch (err) {
    console.error('update resource failed', err);
    res.status(500).json({ error: 'Could not update the resource' });
  }
});

// DELETE /api/resources/:id - authors may remove only their own resource.
app.delete('/api/resources/:id', async (req, res) => {
  const decoded = await verifyFirebaseRequest(req, res);
  if (!decoded) return;
  if (!validResourceId(req.params.id)) return res.status(400).json({ error: 'Invalid resource id' });
  try {
    const ref = db.collection('resources').doc(req.params.id);
    const snapshot = await ref.get();
    if (!snapshot.exists) return res.status(404).json({ error: 'Resource not found' });
    if (snapshot.get('addedBy') !== decoded.uid) return res.status(403).json({ error: 'You cannot delete this resource' });
    await ref.delete();
    res.status(204).end();
  } catch (err) {
    console.error('delete resource failed', err);
    res.status(500).json({ error: 'Could not delete the resource' });
  }
});


return app;
}
module.exports = {createApp};
