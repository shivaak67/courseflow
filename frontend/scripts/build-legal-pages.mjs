import { mkdir, readFile, writeFile } from 'node:fs/promises';

// Use the same templates as Angular so public and in-app policies cannot diverge.
for (const [route, title] of [['privacy', 'Privacy Policy'], ['terms', 'SMS Terms & Conditions']]) {
  const template = await readFile(new URL(`../src/app/features/legal/${route}.component.html`, import.meta.url), 'utf8');
  const output = new URL(`../dist/prioritize/browser/${route}/`, import.meta.url);
  await mkdir(output, { recursive: true });
  await writeFile(new URL('index.html', output), `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>${title} | Prioritize</title><link rel="canonical" href="https://theprioritize.com/${route}">
<link rel="icon" href="/favicon.ico">
<style>body{margin:0;background:#f1f6f5;color:#12262c;font:17px/1.7 system-ui,sans-serif}.legal{max-width:44rem;margin:2rem auto;padding:2rem;background:#fff;border:1px solid #dce5e3;border-radius:20px}a{color:#006b67}h1,h2{font-family:Georgia,serif;line-height:1.25}h1{font-size:2.4rem}h2{font-size:1.35rem;margin-top:2rem}@media(max-width:600px){.legal{margin:1rem;padding:1.25rem}h1{font-size:2rem}}</style>
</head><body><main>${template}</main></body></html>`);
}
