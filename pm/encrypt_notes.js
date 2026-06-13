#!/usr/bin/env node
// Encrypt teacher notes for the password-gated section. Output is WebCrypto-
// compatible (PBKDF2-SHA256 + AES-256-GCM, ciphertext||tag), so the browser
// decrypts it with the same password. The repo only ever stores ciphertext.
//
// Usage: node pm/encrypt_notes.js <password> <infile.md> <outfile.enc>
const fs = require('fs'), crypto = require('crypto');
const [, , pw, inf, outf] = process.argv;
if (!pw || !inf || !outf) { console.error('usage: encrypt_notes.js <password> <in.md> <out.enc>'); process.exit(1); }
const data = fs.readFileSync(inf);
const salt = crypto.randomBytes(16), iv = crypto.randomBytes(12), iter = 200000;
const key = crypto.pbkdf2Sync(pw, salt, iter, 32, 'sha256');
const c = crypto.createCipheriv('aes-256-gcm', key, iv);
const ct = Buffer.concat([c.update(data), c.final()]);
const full = Buffer.concat([ct, c.getAuthTag()]);  // WebCrypto expects tag appended
const b64 = b => b.toString('base64');
fs.mkdirSync(require('path').dirname(outf), { recursive: true });
fs.writeFileSync(outf, JSON.stringify({ salt: b64(salt), iv: b64(iv), iter, ct: b64(full) }));
console.log(`wrote ${outf} (${full.length} bytes ciphertext, ${iter} PBKDF2 iters)`);
