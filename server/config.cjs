function readConfig(env = process.env) {
  const rawPort = env.PORT || '8080';
  if (!/^\d+$/.test(rawPort) || Number(rawPort) < 1 || Number(rawPort) > 65535) throw new Error('PORT must be an integer between 1 and 65535');
  let serviceAccount = null;
  if (env.FIREBASE_SERVICE_ACCOUNT?.trim()) {
    try { serviceAccount = JSON.parse(env.FIREBASE_SERVICE_ACCOUNT); } catch { throw new Error('FIREBASE_SERVICE_ACCOUNT must contain valid JSON'); }
    if (!serviceAccount || typeof serviceAccount !== 'object' ||
        !['project_id', 'client_email', 'private_key'].every(key => typeof serviceAccount[key] === 'string' && serviceAccount[key].trim())) {
      throw new Error('FIREBASE_SERVICE_ACCOUNT needs project_id, client_email and private_key');
    }
  }
  return { port: Number(rawPort), serviceAccount };
}
module.exports = { readConfig };
