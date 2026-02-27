const fs = require('fs');
const path = require('path');

const rootDir = path.join(__dirname, '..');
const pkgPath = path.join(rootDir, 'package.json');
const pomPath = path.join(rootDir, 'pom.xml');

// Bump package.json
const pkg = JSON.parse(fs.readFileSync(pkgPath, 'utf8'));
let [major, minor, patch] = pkg.version.replace('-SNAPSHOT', '').split('.');
patch = parseInt(patch) + 1;
pkg.version = `${major}.${minor}.${patch}`;
fs.writeFileSync(pkgPath, JSON.stringify(pkg, null, 2) + '\n');

// Bump pom.xml
let pom = fs.readFileSync(pomPath, 'utf8');

let pomReplaced = false;
pom = pom.replace(/(<artifactId>extraction-region-editor<\/artifactId>\s*\n\s*<version>)(\d+)\.(\d+)\.(\d+)(-SNAPSHOT)?(<\/version>)/, (match, prefix, p1, p2, p3, snapshot, suffix) => {
    pomReplaced = true;
    return `${prefix}${p1}.${p2}.${parseInt(p3) + 1}${snapshot || ''}${suffix}`;
});

if (pomReplaced) {
    fs.writeFileSync(pomPath, pom);
    console.log(`\n\x1b[36m[ExtractionRegionEditor Tooling] \x1b[32mVersion bumped to ${major}.${minor}.${patch}-SNAPSHOT! \x1b[0m\n`);
} else {
    console.error('\n\x1b[31m[ExtractionRegionEditor Tooling] Failed to find project version in pom.xml to bump!\x1b[0m\n');
}
