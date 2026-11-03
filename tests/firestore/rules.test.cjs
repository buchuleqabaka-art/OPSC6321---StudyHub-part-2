const {before, after, beforeEach, test} = require('node:test');
const fs = require('node:fs');
const {initializeTestEnvironment, assertSucceeds, assertFails} = require('@firebase/rules-unit-testing');
const {doc, setDoc, getDoc, updateDoc, deleteDoc, collection, query, where, getDocs} = require('firebase/firestore');
let env;
const db = uid => env.authenticatedContext(uid).firestore();
const task = (extra={}) => ({title:'Study',description:'',dueDate:9999999999999,assignedToId:'bob',assignedToName:'Bob',groupId:'group',priority:'MEDIUM',completed:false,ownerId:'alice',...extra});
const session = (extra={}) => ({title:'Revision',description:'',groupId:'group',startTime:9999999999000,endTime:9999999999999,location:'Library',attendeeIds:['alice'],ownerId:'alice',...extra});
before(async()=>{env=await initializeTestEnvironment({projectId:'demo-studyhub',firestore:{rules:fs.readFileSync('firestore.rules','utf8')}});});
after(async()=>{await env.cleanup();});
beforeEach(async()=>{
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async context=>{
    const store=context.firestore();
    await setDoc(doc(store,'groups/group'),{name:'Study',ownerId:'alice',memberIds:['alice','bob','charlie']});
    await setDoc(doc(store,'tasks/task'),task());
    await setDoc(doc(store,'sessions/session'),session());
    await setDoc(doc(store,'tasks/private'),task({groupId:'',assignedToId:'alice'}));
    await setDoc(doc(store,'sessions/private'),session({groupId:''}));
  });
});
test('members can read shared tasks and sessions; outsiders cannot',async()=>{
  for(const path of ['tasks/task','sessions/session']) {
    await assertSucceeds(getDoc(doc(db('bob'),path)));
    await assertFails(getDoc(doc(db('eve'),path)));
  }
});
test('private planning stays private',async()=>{
  for(const path of ['tasks/private','sessions/private']) {
    await assertSucceeds(getDoc(doc(db('alice'),path)));
    await assertFails(getDoc(doc(db('bob'),path)));
  }
});
test('owner and group queries are permitted',async()=>{
  await assertSucceeds(getDocs(query(collection(db('alice'),'tasks'),where('ownerId','==','alice'))));
  await assertSucceeds(getDocs(query(collection(db('bob'),'tasks'),where('groupId','==','group'))));
  await assertSucceeds(getDocs(query(collection(db('bob'),'sessions'),where('groupId','==','group'))));
});
test('assignee can complete but cannot change owner title or assignment',async()=>{
  await assertSucceeds(updateDoc(doc(db('bob'),'tasks/task'),{completed:true}));
  await assertFails(updateDoc(doc(db('bob'),'tasks/task'),{ownerId:'bob'}));
  await assertFails(updateDoc(doc(db('bob'),'tasks/task'),{assignedToId:'charlie'}));
  await assertFails(updateDoc(doc(db('bob'),'tasks/task'),{title:'Overwrite'}));
  await assertFails(updateDoc(doc(db('charlie'),'tasks/task'),{completed:false}));
});
test('only owner deletes a task',async()=>{
  await assertFails(deleteDoc(doc(db('bob'),'tasks/task')));
  await assertSucceeds(deleteDoc(doc(db('alice'),'tasks/task')));
});
test('assignment requires membership and title validates',async()=>{
  await assertSucceeds(setDoc(doc(db('alice'),'tasks/valid'),task()));
  await assertFails(setDoc(doc(db('alice'),'tasks/outsider'),task({assignedToId:'eve'})));
  await assertFails(setDoc(doc(db('alice'),'tasks/blank'),task({title:''})));
  await assertFails(setDoc(doc(db('alice'),'tasks/long'),task({title:'x'.repeat(161)})));
  await assertFails(setDoc(doc(db('alice'),'tasks/finished'),task({completed:true})));
});
test('session end must follow start and member may only change own attendance',async()=>{
  await assertSucceeds(setDoc(doc(db('alice'),'sessions/new'),session()));
  await assertFails(setDoc(doc(db('alice'),'sessions/bad'),session({endTime:50})));
  await assertSucceeds(updateDoc(doc(db('bob'),'sessions/session'),{attendeeIds:['alice','bob']}));
  await assertFails(updateDoc(doc(db('bob'),'sessions/session'),{attendeeIds:['bob']}));
  await assertFails(updateDoc(doc(db('eve'),'sessions/session'),{attendeeIds:['alice','bob','eve']}));
});
test('member directory does not expose private profiles or permit impersonation',async()=>{
  await assertSucceeds(setDoc(doc(db('bob'),'groups/group/members/bob'),{name:'Bob'}));
  await assertFails(setDoc(doc(db('bob'),'groups/group/members/alice'),{name:'Fake'}));
  await assertSucceeds(getDoc(doc(db('alice'),'groups/group/members/bob')));
  await assertFails(getDoc(doc(db('eve'),'groups/group/members/bob')));
  await assertFails(getDoc(doc(db('alice'),'users/bob')));
});
test('removed member loses read and completion access',async()=>{
  await env.withSecurityRulesDisabled(context=>updateDoc(doc(context.firestore(),'groups/group'),{memberIds:['alice']}));
  await assertFails(getDoc(doc(db('bob'),'tasks/task')));
  await assertFails(updateDoc(doc(db('bob'),'tasks/task'),{completed:true}));
});
