const admin = require('firebase-admin');
const {readConfig} = require('./server/config.cjs');
const {createApp} = require('./server/app.cjs');
const config = readConfig();
if (config.serviceAccount) admin.initializeApp({credential: admin.credential.cert(config.serviceAccount)});
else admin.initializeApp();
const app = createApp({admin, db: admin.firestore()});
app.listen(config.port, '0.0.0.0', () => console.log(`StudyHub API listening on ${config.port}`));
