#!/usr/bin/env node
// Compares two spring-test-profiler HTML reports visually.
//
// Both reports contain run-specific content (timestamps, durations, memory figures,
// version strings) that would show up as noise in a pixel comparison. The script
// normalizes that content in both files, renders each in headless Chromium, and
// pixel-compares the full-page screenshots.
//
// Exit code 0 regardless of the visual diff (report-only check); non-zero only on
// crashes (missing input, browser failure).

import { createServer } from 'node:http';
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { basename, resolve } from 'node:path';
import { PNG } from 'pngjs';
import pixelmatch from 'pixelmatch';
import { chromium } from 'playwright';

const FIXED_TIMESTAMP = '2000-01-01 00:00:00';
const FIXED_EPOCH_SECONDS = 946684800;
const FIXED_LOAD_DURATION_MS = 1000;
const VIEWPORT = { width: 1400, height: 900 };
const D3_SETTLE_MS = 2000;
const PIXELMATCH_THRESHOLD = 0.1;
const MINOR_DIFF_RATIO = 0.005;

function parseArgs(argv) {
  const args = {};
  for (let i = 0; i < argv.length; i += 2) {
    const key = argv[i];
    const value = argv[i + 1];
    if (!key?.startsWith('--') || value === undefined) {
      throw new Error(`Invalid argument pair: ${key} ${value}`);
    }
    args[key.slice(2)] = value;
  }
  const required = ['baseline', 'current', 'out', 'baseline-version', 'current-version'];
  for (const name of required) {
    if (!args[name]) {
      throw new Error(
        `Missing --${name}. Usage: node compare-reports.mjs --baseline <html> --current <html> ` +
          '--out <dir> --baseline-version <v> --current-version <v>'
      );
    }
  }
  return args;
}

function escapeRegExp(text) {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

// The report assigns "context-N" keys in test execution order, which is not stable
// across runs. Sorts the embedded statistics entries by their content and renumbers
// the keys, then applies the same renumbering to every "context-N" token in the HTML
// (the caching section displays them and the report JS cross-references them).
function canonicalizeStatisticsJson(html) {
  const keyMapping = new Map();

  let normalized = html.replace(
    /(<script type="application\/json" id="context-statistics-json"[^>]*>)([\s\S]*?)(<\/script>)/,
    (match, openTag, jsonText, closeTag) => {
      const statistics = JSON.parse(jsonText.trim() || '[]');
      const normalizeEntry = (entry) => {
        if (Array.isArray(entry)) {
          entry.forEach(normalizeEntry);
        } else if (entry && typeof entry === 'object') {
          if ('loadDuration' in entry) entry.loadDuration = FIXED_LOAD_DURATION_MS;
          if ('initialLoadTime' in entry) entry.initialLoadTime = FIXED_EPOCH_SECONDS;
          if ('lastUsedTime' in entry) entry.lastUsedTime = FIXED_EPOCH_SECONDS;
          Object.values(entry).forEach(normalizeEntry);
        }
      };
      normalizeEntry(statistics);
      const contentKey = (entry) => JSON.stringify({ ...entry, contextKey: undefined });
      statistics.sort((a, b) => contentKey(a).localeCompare(contentKey(b)));
      statistics.forEach((entry, index) => {
        if (entry?.contextKey) {
          keyMapping.set(entry.contextKey, `context-${index}`);
        }
      });
      return openTag + JSON.stringify(statistics) + closeTag;
    }
  );

  // Two-pass replacement so overlapping renames (context-1 -> context-3 while
  // context-3 -> context-1) cannot clobber each other.
  let placeholderIndex = 0;
  const placeholders = [];
  for (const [oldKey, newKey] of keyMapping) {
    const placeholder = `@@context-placeholder-${placeholderIndex++}@@`;
    placeholders.push([placeholder, newKey]);
    normalized = normalized.replace(new RegExp(`\\b${escapeRegExp(oldKey)}\\b`, 'g'), placeholder);
  }
  for (const [placeholder, newKey] of placeholders) {
    normalized = normalized.replaceAll(placeholder, newKey);
  }
  return normalized;
}

function normalizeHtml(html, versionTokens) {
  let normalized = html;

  // Fix the D3 chart inputs and context identities first so chart geometry and
  // context labels are deterministic.
  normalized = canonicalizeStatisticsJson(normalized);

  // Version strings (footer <span> and utm_content query parameters).
  for (const version of versionTokens) {
    normalized = normalized.replaceAll(version, 'X.Y.Z');
  }

  // Generated-at timestamps (header, footer, summary "Execution Time").
  normalized = normalized.replace(/\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}:\d{2}/g, FIXED_TIMESTAMP);

  // Heap memory stat rows render only when heapMemoryUsedBytes > 0, so one report
  // may contain them and the other not; drop them entirely to avoid layout noise.
  normalized = normalized.replace(
    /<div class="stat-row">\s*<span class="stat-label">Heap Memory:<\/span>[\s\S]*?<\/div>/g,
    ''
  );

  // Load-time data attributes consumed by the report JS.
  normalized = normalized.replace(/data-load-time-ms="\d+"/g, 'data-load-time-ms="0"');

  // DurationFormatter output: "123ms", "1.2s", "1.5m" (plus raw "...ms" concatenations).
  normalized = normalized.replace(/\b\d+(\.\d+)?(ms|s|m)\b/g, '0ms');

  // Any remaining memory figures.
  normalized = normalized.replace(/\b\d+(\.\d+)?\s?MB\b/g, '0.0MB');

  return normalized;
}

function serveDirectory(directory) {
  return new Promise((resolvePromise) => {
    const server = createServer((request, response) => {
      try {
        const content = readFileSync(resolve(directory, '.' + request.url));
        response.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
        response.end(content);
      } catch {
        response.writeHead(404);
        response.end('Not found');
      }
    });
    server.listen(0, '127.0.0.1', () => resolvePromise(server));
  });
}

async function screenshotPage(browser, url, outputPath) {
  const page = await browser.newPage({ viewport: VIEWPORT, deviceScaleFactor: 1 });
  await page.goto(url, { waitUntil: 'networkidle' });
  await page.waitForTimeout(D3_SETTLE_MS);

  // Cache entries and context configurations are rendered from unordered maps, so
  // their document order differs between runs. Sort them by content after the report
  // JS has rendered, and renumber the display-only "config-N" labels.
  await page.evaluate(() => {
    const sortSiblings = (selector, keyOf) => {
      const byParent = new Map();
      for (const element of document.querySelectorAll(selector)) {
        const siblings = byParent.get(element.parentElement) ?? [];
        siblings.push(element);
        byParent.set(element.parentElement, siblings);
      }
      for (const [parent, elements] of byParent) {
        elements.sort((a, b) => keyOf(a).localeCompare(keyOf(b), 'en', { numeric: true }));
        for (const element of elements) {
          parent.appendChild(element);
        }
      }
    };
    sortSiblings('.cache-entry', (el) => el.querySelector('.cache-id')?.textContent ?? el.textContent);
    sortSiblings('.context-config-item', (el) => el.textContent.replace(/config-\d+/g, ''));
    document
      .querySelectorAll('.context-config-item .context-config-id span')
      .forEach((span, index) => (span.textContent = `config-${index}`));
  });

  await page.screenshot({ path: outputPath, fullPage: true, animations: 'disabled' });
  await page.close();
}

// Pads an image to the target size with magenta so added/removed content at the
// bottom of the longer report counts as a visual difference.
function padImage(image, targetWidth, targetHeight) {
  if (image.width === targetWidth && image.height === targetHeight) {
    return image;
  }
  const padded = new PNG({ width: targetWidth, height: targetHeight });
  for (let y = 0; y < targetHeight; y++) {
    for (let x = 0; x < targetWidth; x++) {
      const targetIndex = (targetWidth * y + x) << 2;
      if (x < image.width && y < image.height) {
        const sourceIndex = (image.width * y + x) << 2;
        padded.data[targetIndex] = image.data[sourceIndex];
        padded.data[targetIndex + 1] = image.data[sourceIndex + 1];
        padded.data[targetIndex + 2] = image.data[sourceIndex + 2];
        padded.data[targetIndex + 3] = image.data[sourceIndex + 3];
      } else {
        padded.data[targetIndex] = 255;
        padded.data[targetIndex + 1] = 0;
        padded.data[targetIndex + 2] = 255;
        padded.data[targetIndex + 3] = 255;
      }
    }
  }
  return padded;
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const outputDir = resolve(args.out);
  mkdirSync(outputDir, { recursive: true });

  const versionTokens = [args['baseline-version'], args['current-version']];
  const inputs = [
    { label: 'baseline', htmlPath: resolve(args.baseline) },
    { label: 'current', htmlPath: resolve(args.current) },
  ];

  for (const input of inputs) {
    const html = readFileSync(input.htmlPath, 'utf8');
    input.normalizedName = `${input.label}.normalized.html`;
    writeFileSync(resolve(outputDir, input.normalizedName), normalizeHtml(html, versionTokens));
  }

  const server = await serveDirectory(outputDir);
  const port = server.address().port;
  const browser = await chromium.launch();
  try {
    for (const input of inputs) {
      input.screenshotPath = resolve(outputDir, `${input.label}.png`);
      await screenshotPage(browser, `http://127.0.0.1:${port}/${input.normalizedName}`, input.screenshotPath);
      console.log(`Captured ${basename(input.screenshotPath)}`);
    }
  } finally {
    await browser.close();
    server.close();
  }

  const baselineImage = PNG.sync.read(readFileSync(inputs[0].screenshotPath));
  const currentImage = PNG.sync.read(readFileSync(inputs[1].screenshotPath));
  const width = Math.max(baselineImage.width, currentImage.width);
  const height = Math.max(baselineImage.height, currentImage.height);
  const paddedBaseline = padImage(baselineImage, width, height);
  const paddedCurrent = padImage(currentImage, width, height);

  const diffImage = new PNG({ width, height });
  const diffPixels = pixelmatch(paddedBaseline.data, paddedCurrent.data, diffImage.data, width, height, {
    threshold: PIXELMATCH_THRESHOLD,
  });
  writeFileSync(resolve(outputDir, 'diff.png'), PNG.sync.write(diffImage));

  const totalPixels = width * height;
  const diffRatio = diffPixels / totalPixels;
  const classification =
    diffPixels === 0 ? 'identical' : diffRatio < MINOR_DIFF_RATIO ? 'minor' : 'notable';

  const summary = {
    baselineVersion: args['baseline-version'],
    currentVersion: args['current-version'],
    width,
    height,
    totalPixels,
    diffPixels,
    diffRatio,
    classification,
  };
  writeFileSync(resolve(outputDir, 'summary.json'), JSON.stringify(summary, null, 2) + '\n');

  console.log(
    `Visual comparison: ${diffPixels} of ${totalPixels} pixels differ ` +
      `(${(diffRatio * 100).toFixed(3)}%) - ${classification}`
  );
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
