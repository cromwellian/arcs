/**
 * @license
 * Copyright (c) 2020 Google Inc. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt
 * Code distributed by Google as part of this project is also
 * subject to an additional IP rights grant found at
 * http://polymer.github.io/PATENTS.txt
 */
import minimist from 'minimist';
import fs from 'fs';
import path from 'path';
import {Runtime} from '../runtime/runtime.js';
import {recipe2plan} from './recipe2plan.js';

const opts = minimist(process.argv.slice(2), {
  string: ['outdir', 'outfile', 'package'],
  alias: {d: 'outdir', f: 'outfile', p: 'package'},
  default: {outdir: '.'}
});

if (opts.help || opts._.length === 0) {
  console.log(`
Usage
  $ tools/sigh recipe2plan [options] path/to/manifest.arcs

Description
  Generates Kotlin plans from recipes in a manifest. 

Options
  --outfile, -f output filename; required
  --outdir, -d  output directory; defaults to '.'
  --package, -p kotlin package.
  --help        usage info
`);
  process.exit(0);
}

if (!opts.outfile) {
  console.error(`Parameter --outfile is required.`);
  process.exit(1);
}


if (!opts.package) {
  console.error(`Parameter --package is required.`);
  process.exit(1);
}

// TODO(alxr): Support generation from multiple manifests
if (opts._.length > 1) {
  console.error(`Only a single manifest is allowed`);
  process.exit(1);
}

if (opts._.some((file) => !file.endsWith('.arcs'))) {
  console.error(`Only Arcs manifests ('*.arcs') are allowed.`);
  process.exit(1);
}

async function main() {
  try {
    Runtime.init('../..');
    fs.mkdirSync(opts.outdir, {recursive: true});

    const plans = await recipe2plan(opts._[0], opts.package);

    const outPath = path.join(opts.outdir, opts.outfile);
    console.log(outPath);

    const outFile = fs.openSync(outPath, 'w');
    fs.writeSync(outFile, plans);
    fs.closeSync(outFile);
  } catch (e) {
    console.error(e);
    process.exit(1);
  }
}

void main();