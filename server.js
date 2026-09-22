// StudyHub REST API


const express = require('express');
const cors = require('cors');
const admin = require('firebase-admin');

const app = express();
app.use(cors());
app.use(express.json());

// Service account comes from an environment variable so no keys sit in git
const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT || '{}');
if (serviceAccount.project_id) {
  admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
} else {
  admin.initializeApp(); // uses application default credentials
}
const db = admin.firestore();

// Health check - useful for the demo video and for uptime pings
app.get('/', (req, res) => {
  res.json({ service: 'StudyHub API', status: 'ok', time: new Date().toISOString() });
});

// GET /api/quote/today - one rotating quote per day, stored in Firestore
app.get('/api/quote/today', async (req, res) => {
  try {
    const snapshot = await db.collection('quotes').get();
    if (snapshot.empty) {
      return res.json({
        id: 'default',
        content: 'Small consistent effort beats one long night before the deadline.',
        author: 'StudyHub'
      });
    }
    const docs = snapshot.docs;
    // Same quote for everyone for a given day
    const dayIndex = Math.floor(Date.now() / 86400000) % docs.length;
    const doc = docs[dayIndex];
    res.json({ id: doc.id, ...doc.data() });
  } catch (err) {
    console.error('quote failed', err);
    res.status(500).json({ error: 'Could not load the quote of the day' });
  }
});

// GET /api/resources?subject=Databases
app.get('/api/resources', async (req, res) => {
  try {
    let query = db.collection('resources');
    if (req.query.subject) query = query.where('subject', '==', req.query.subject);
    const snapshot = await query.limit(50).get();
    res.json(snapshot.docs.map((d) => ({ id: d.id, ...d.data() })));
  } catch (err) {
    console.error('resources failed', err);
    res.status(500).json({ error: 'Could not load resources' });
  }
});

// POST /api/resources
app.post('/api/resources', async (req, res) => {
  const { title, subject, url, addedBy } = req.body || {};
  if (!title || !url) {
    return res.status(400).json({ error: 'title and url are required' });
  }
  try {
    const ref = await db.collection('resources').add({
      title, subject: subject || '', url, addedBy: addedBy || '',
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
    res.status(201).json({ id: ref.id, title, subject, url, addedBy });
  } catch (err) {
    console.error('create resource failed', err);
    res.status(500).json({ error: 'Could not save the resource' });
  }
});

// PUT /api/resources/:id
app.put('/api/resources/:id', async (req, res) => {
  try {
    await db.collection('resources').doc(req.params.id).update(req.body);
    res.json({ id: req.params.id, ...req.body });
  } catch (err) {
    res.status(404).json({ error: 'Resource not found' });
  }
});

// DELETE /api/resources/:id
app.delete('/api/resources/:id', async (req, res) => {
  try {
    await db.collection('resources').doc(req.params.id).delete();
    res.status(204).send();
  } catch (err) {
    res.status(404).json({ error: 'Resource not found' });
  }
});

const PORT = process.env.PORT || 8080;
app.listen(PORT, () => console.log(`StudyHub API listening on ${PORT}`));