// Offline checks for standalone and inline JavaScript, and static asset/page links.
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const root = path.resolve(__dirname, 'src/main/resources/static');
let javascriptBlocks = 0, localReferences = 0, failures = 0;
function checkScript(source, filename) {
  javascriptBlocks++;
  try { new vm.Script(source, { filename }); }
  catch (error) { failures++; console.error(`${filename}: ${error.message}`); }
}
function walk(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const filename = path.join(dir, entry.name);
    if (entry.isDirectory()) { walk(filename); continue; }
    if (filename.endsWith('.js')) checkScript(fs.readFileSync(filename, 'utf8'), filename);
    if (!filename.endsWith('.html')) continue;
    const html = fs.readFileSync(filename, 'utf8');
    for (const [index, match] of [...html.matchAll(/<script\b([^>]*)>([\s\S]*?)<\/script>/gi)].entries()) {
      if (/\bsrc\s*=|application\/ld\+json|application\/json/i.test(match[1])) continue;
      checkScript(match[2], `${filename}:inline-${index}`);
    }
    for (const match of html.matchAll(/(?:src|href)=["']([^"'#?]+)["']/gi)) {
      const reference = match[1];
      // Template literals are resolved at runtime from API response data.
      if (/^(https?:|data:|mailto:|javascript:|tel:|\/api\/)/.test(reference) || reference.includes('${')) continue;
      localReferences++;
      const target = reference.startsWith('/') ? path.join(root, reference.slice(1)) : path.resolve(dir, reference);
      if (!fs.existsSync(target)) { failures++; console.error(`Missing ${reference} in ${filename}`); }
    }
  }
}
walk(root);
console.log(JSON.stringify({ javascriptBlocks, localReferences, failures }));
process.exitCode = failures ? 1 : 0;
