const {before,after,beforeEach,test}=require('node:test');
const assert=require('node:assert/strict');
const admin=require('firebase-admin');
const {createApp}=require('../../server/app.cjs');
let db, server, base, sent=[];
before(async()=>{
  assert.ok(process.env.FIRESTORE_EMULATOR_HOST,'Use Firestore emulator, never production');
  admin.initializeApp({projectId:'demo-studyhub-api'});
  db=admin.firestore();
  const facade={firestore:admin.firestore,
    auth:()=>({verifyIdToken:async token=>{if(!['alice-valid','bob-valid','eve-valid'].includes(token))throw Error('invalid');return {uid:token.split('-')[0]};}}),
    messaging:()=>({sendEach:async messages=>{sent.push(...messages);return {responses:messages.map(()=>({success:true}))};}})};
  const app=createApp({admin:facade,db,env:{}});
  server=app.listen(0,'127.0.0.1');await new Promise(resolve=>server.once('listening',resolve));base=`http://127.0.0.1:${server.address().port}`;
});
after(async()=>{await new Promise(resolve=>server.close(resolve));await admin.app().delete();});
beforeEach(async()=>{sent=[];for(const name of ['resources','groups','users','quotes','quizUsage'])await db.recursiveDelete(db.collection(name));});
async function request(path,method='GET',body,token='alice-valid') {
  return fetch(base+path,{method,headers:{'Content-Type':'application/json',Authorization:'Bearer '+token},...(body===undefined?{}:{body:JSON.stringify(body)})});
}
test('health and localized daily quote fallback and stored quote',async()=>{
  assert.equal((await request('/')).status,200);
  for(const lang of ['en','af','zu']) {const r=await request('/api/quote/today?lang='+lang);const q=await r.json();assert.equal(q.language,lang);assert.ok(q.content);}
  await db.collection('quotes').doc('one').set({content:'Study daily',author:'Tutor',translations:{af:{content:'Leer daagliks'}}});
  assert.equal((await (await request('/api/quote/today?lang=af')).json()).content,'Leer daagliks');
});
test('resource lifecycle validates input, requires auth and protects other authors',async()=>{
  const value={title:'Database notes',subject:'DATA',url:'https://example.org/notes'};
  assert.equal((await request('/api/resources','POST',value,'bad')).status,401);
  for(const bad of [{...value,title:''},{...value,url:'javascript:alert(1)'},{...value,url:'https://user:password@example.com'}])assert.equal((await request('/api/resources','POST',bad)).status,400);
  const response=await request('/api/resources','POST',value);assert.equal(response.status,201);const resource=await response.json();
  assert.equal(resource.addedBy,'alice');
  assert.equal((await (await request('/api/resources?subject=DATA')).json()).length,1);
  assert.equal((await request('/api/resources/'+resource.id,'PUT',value,'bob-valid')).status,403);
  assert.equal((await request('/api/resources/'+resource.id,'DELETE',undefined,'bob-valid')).status,403);
  assert.equal((await request('/api/resources/'+resource.id,'PUT',{...value,title:'Updated'})).status,200);
  assert.equal((await request('/api/resources/'+resource.id,'DELETE')).status,204);
  assert.equal((await request('/api/resources/'+resource.id,'DELETE')).status,404);
});
test('notification registration, sender authorization, recipients and unregister',async()=>{
  const token='device-token-'.repeat(5);
  assert.equal((await request('/api/notifications/register','POST',{token:'short'})).status,400);
  assert.equal((await request('/api/notifications/register','POST',{token},'invalid')).status,401);
  assert.equal((await request('/api/notifications/register','POST',{token},'bob-valid')).status,204);
  await db.doc('groups/g').set({memberIds:['alice','bob']});
  await db.doc('groups/g/messages/m').set({senderId:'alice',senderName:'Alice',text:'Hello'});
  const payload={groupId:'g',messageId:'m'};
  assert.equal((await request('/api/notifications/group-message','POST',payload,'eve-valid')).status,403);
  assert.equal((await request('/api/notifications/group-message','POST',payload,'bob-valid')).status,403);
  assert.equal((await request('/api/notifications/group-message','POST',{groupId:'a/b',messageId:'m'})).status,400);
  assert.equal((await request('/api/notifications/group-message','POST',payload)).status,204);
  assert.equal(sent.length,1);assert.equal(sent[0].data.recipientUid,'bob');assert.equal(sent[0].token,token);
  assert.equal((await request('/api/notifications/unregister','POST',{token},'bob-valid')).status,204);
  assert.equal((await db.collection('users/bob/deviceTokens').get()).size,0);
});
test('quiz status is authenticated and absent key yields explicit setup response',async()=>{
  assert.equal((await request('/api/quizzes/status','GET',undefined,'invalid')).status,401);
  assert.equal((await (await request('/api/quizzes/status')).json()).configured,false);
  assert.equal((await request('/api/quizzes/generate?count=5&lang=en','POST',{})).status,503);
});
