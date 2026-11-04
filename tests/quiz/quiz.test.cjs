const { test } = require('node:test');
const assert = require('node:assert/strict');
const { validateQuiz, generate } = require('../../server/quiz.cjs');
const valid = () => ({ title: 'Databases', questions: Array.from({ length: 5 }, (_, i) => ({ prompt: `Question ${i}`, options: ['A','B','C','D'], answer: 0, explanation: 'Because of the key.', source: `Question ${i + 1}` })) });
test('valid quiz and invalid answer/options/count/source', () => {
  assert.equal(validateQuiz(valid(), 5).questions.length, 5);
  for (const mutate of [q => q.questions[0].answer = 4, q => q.questions[0].options[1] = 'A', q => q.questions.pop(), q => q.questions[0].source = '', q => q.questions[1].prompt = 'Question 0']) {
    const q = valid(); mutate(q); assert.throws(() => validateQuiz(q, 5));
  }
});
test('provider request uses server auth, PDF input, strict output and no storage', async () => {
  const quiz = await generate(Buffer.from('%PDF-1.4'), 5, 'English', { OPENAI_API_KEY: 'test-key' }, async (url, options) => {
    assert.equal(url, 'https://api.openai.com/v1/responses');
    const body = JSON.parse(options.body);
    assert.equal(body.store, false); assert.equal(body.text.format.strict, true);
    assert.match(body.input[0].content[0].file_data, /^data:application\/pdf;base64,/);
    assert.equal(options.headers.Authorization, 'Bearer test-key');
    return { ok: true, json: async () => ({ status: 'completed', output: [{ content: [{ type: 'output_text', text: JSON.stringify(valid()) }] }] }) };
  });
  assert.equal(quiz.questions.length, 5);
});
test('provider refusal, incomplete and malformed responses fail safely', async () => {
  for (const result of [{status:'incomplete'}, {status:'completed',output:[]}, {status:'completed',output:[{content:[{type:'output_text',text:'bad JSON'}]}]}]) {
    await assert.rejects(generate(Buffer.from('%PDF-1.4'), 5, 'English', {}, async () => ({ok:true,json:async()=>result})));
  }
  await assert.rejects(generate(Buffer.from('%PDF-1.4'), 5, 'English', {}, async () => ({ok:false})));
});
test('HTTP endpoint checks authentication, setup, size, PDF, options, quota and generator failures', async () => {
  const express = require('express');
  const { registerQuiz } = require('../../server/quiz.cjs');
  const app = express(); let allowed = true; let fail = false; let calls = 0;
  const env = { OPENAI_API_KEY: 'test' };
  registerQuiz(app, { express, env, verify: async (req,res) => {
    if (req.headers.authorization !== 'Bearer test') { res.sendStatus(401); return null; }
    return {uid:'alice'};
  }, reserve: async uid => { assert.equal(uid,'alice'); return allowed; }, generator: async () => { calls++; if(fail) throw new Error(); return valid(); } });
  const server = app.listen(0, '127.0.0.1');
  await new Promise(resolve => server.once('listening', resolve));
  const url = `http://127.0.0.1:${server.address().port}/api/quizzes/generate`;
  const request = (body='%PDF-1.4\n', query='?count=5&lang=en', auth='Bearer test') => fetch(url+query, {method:'POST',headers:{'Content-Type':'application/pdf',Authorization:auth},body});
  try {
    assert.equal((await request(undefined,undefined,'')).status,401);
    delete env.OPENAI_API_KEY; assert.equal((await request()).status,503); env.OPENAI_API_KEY='test';
    assert.equal((await request('not a PDF')).status,400);
    assert.equal((await request(undefined,'?count=50&lang=en')).status,400);
    assert.equal((await request(undefined,'?count=5&lang=bad')).status,400);
    assert.equal((await request(undefined,'?count=5&lang=__proto__')).status,400);
    assert.equal((await request(Buffer.alloc(5*1024*1024+1))).status,413);
    assert.equal(calls,0);
    allowed=false; assert.equal((await request()).status,429); allowed=true;
    const response=await request(); assert.equal(response.status,200); assert.equal((await response.json()).questions.length,5);
    fail=true; assert.equal((await request()).status,502);
  } finally { await new Promise(resolve => server.close(resolve)); }
});
