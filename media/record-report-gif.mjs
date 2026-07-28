// Records the report walkthrough GIF source video and captures the marketing
// screenshots (context timeline for LinkedIn, results.json for the newsletter).
// Server/scroll/highlight helpers adapted from video/record-demo.mjs.
// Run via media/record-report-gif.sh, which also converts the webm to a GIF.

import { chromium } from 'playwright';
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PROJECT_DIR = path.resolve(__dirname, '..');
const REPORT_DIR = path.resolve(PROJECT_DIR, 'demo/spring-boot-4.0-maven/target/spring-test-profiler');
const OUTPUT_DIR = path.resolve(__dirname, 'output');
const ASSETS_DIR = path.resolve(PROJECT_DIR, 'docs/marketing/announcements/assets');

const TIMELINE_SCREENSHOT = path.join(ASSETS_DIR, 'context-timeline-screenshot.png');
const RESULTS_JSON_SCREENSHOT = path.join(ASSETS_DIR, 'results-json-screenshot.png');

const GIF_VIEWPORT = { width: 1280, height: 800 };
const SCREENSHOT_VIEWPORT = { width: 1600, height: 900 };
const D3_RENDER_WAIT = 2500;

const gifOnly = process.argv.includes('--gif-only');
const screenshotsOnly = process.argv.includes('--screenshots-only');

const mimeTypes = {
  '.html': 'text/html',
  '.css': 'text/css',
  '.js': 'application/javascript',
  '.json': 'application/json',
  '.png': 'image/png',
  '.svg': 'image/svg+xml',
  '.woff2': 'font/woff2',
  '.woff': 'font/woff',
  '.ttf': 'font/ttf',
};

function startServer() {
  const server = http.createServer((req, res) => {
    let urlPath = req.url.split('?')[0];
    if (urlPath === '/') urlPath = '/latest.html';
    const filePath = path.join(REPORT_DIR, urlPath);
    const ext = path.extname(filePath);

    fs.readFile(filePath, (err, data) => {
      if (err) { res.writeHead(404); res.end('Not found'); return; }
      res.writeHead(200, { 'Content-Type': mimeTypes[ext] || 'application/octet-stream' });
      res.end(data);
    });
  });

  return new Promise(resolve => server.listen(0, () => resolve(server)));
}

async function openReport(page, port) {
  await page.goto(`http://localhost:${port}/latest.html`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(D3_RENDER_WAIT);
  await page.addStyleTag({ content: 'html { scroll-behavior: smooth; }' });
}

// Persistent bottom caption bar; text is swapped with a short crossfade so the
// GIF does not flash between steps.
async function setCaption(page, text) {
  await page.evaluate((captionText) => {
    let bar = document.getElementById('gif-caption-bar');
    if (!bar) {
      bar = document.createElement('div');
      bar.id = 'gif-caption-bar';
      bar.style.cssText = [
        'position: fixed',
        'left: 0',
        'right: 0',
        'bottom: 0',
        'z-index: 99999',
        'background: rgba(30, 36, 48, 0.94)',
        'border-top: 3px solid #6db33f',
        'color: #ffffff',
        'font-family: -apple-system, "Segoe UI", Helvetica, Arial, sans-serif',
        'font-size: 22px',
        'font-weight: 600',
        'line-height: 1.35',
        'text-align: center',
        'padding: 14px 32px',
        'transition: opacity 0.25s ease',
        'opacity: 0',
      ].join(';');
      document.body.appendChild(bar);
    }
    bar.style.opacity = '0';
    setTimeout(() => {
      bar.textContent = captionText;
      bar.style.opacity = '1';
    }, 250);
  }, text);
  await page.waitForTimeout(550);
}

async function highlight(page, selector) {
  await page.evaluate((sel) => {
    const el = document.querySelector(sel);
    if (el) {
      el.style.transition = 'box-shadow 0.5s ease, outline 0.5s ease';
      el.style.boxShadow = '0 0 0 3px rgba(109, 179, 63, 0.4), 0 0 20px rgba(109, 179, 63, 0.15)';
      el.style.outline = '2px solid rgba(109, 179, 63, 0.6)';
      el.style.outlineOffset = '4px';
      el.style.borderRadius = '8px';
    }
  }, selector);
}

async function unhighlight(page, selector) {
  await page.evaluate((sel) => {
    const el = document.querySelector(sel);
    if (el) {
      el.style.boxShadow = '';
      el.style.outline = '';
      el.style.outlineOffset = '';
    }
  }, selector);
}

async function scrollToAndHighlight(page, selector, pause) {
  const element = await page.$(selector);
  if (!element) {
    console.warn(`  Selector not found, skipping: ${selector}`);
    await page.waitForTimeout(pause);
    return;
  }
  await element.scrollIntoViewIfNeeded();
  await page.waitForTimeout(800);
  await highlight(page, selector);
  await page.waitForTimeout(pause);
  await unhighlight(page, selector);
}

async function zoomInto(page, selector, scale, pause) {
  await page.evaluate(([sel, s]) => {
    const el = document.querySelector(sel);
    if (el) {
      el.style.transition = 'transform 0.6s ease';
      el.style.transformOrigin = 'center top';
      el.style.transform = `scale(${s})`;
    }
  }, [selector, scale]);
  await page.waitForTimeout(pause);
  await page.evaluate((sel) => {
    const el = document.querySelector(sel);
    if (el) el.style.transform = '';
  }, selector);
  await page.waitForTimeout(700);
}

async function recordGif(browser, port) {
  console.log('Recording report walkthrough...');

  const context = await browser.newContext({
    viewport: GIF_VIEWPORT,
    recordVideo: { dir: OUTPUT_DIR, size: GIF_VIEWPORT },
  });
  const page = await context.newPage();
  await openReport(page, port);

  // Step 1: intro on the report header
  await setCaption(page, 'Spring Test Profiler - see what your Spring context cache is really doing');
  await page.waitForTimeout(2500);

  // Step 2: summary cards
  await setCaption(page, 'Cache hits, misses, and wasted context time at a glance');
  await scrollToAndHighlight(page, '.summary-grid', 2500);

  // Step 3: cache entries
  await setCaption(page, 'Every ApplicationContext and what it cost');
  await scrollToAndHighlight(page, '.cache-entry', 2700);

  // Step 4: timeline section
  await setCaption(page, 'NEW: Context Cache Timeline (incubating)');
  await scrollToAndHighlight(page, '.context-timeline-section', 2500);

  // Step 5: zoom into the timeline chart
  await setCaption(page, 'Context load, cache lifetime, test executions, and evictions on one chart');
  await zoomInto(page, '.context-timeline-section', 1.15, 4000);

  // Step 6: closing
  await setCaption(page, 'github.com/PragmaTech-GmbH/spring-test-profiler');
  await page.waitForTimeout(2200);

  await page.close();
  await context.close();

  const videos = fs.readdirSync(OUTPUT_DIR).filter(f => f.endsWith('.webm') && f !== 'report-walkthrough.webm');
  if (videos.length === 0) {
    throw new Error('No webm recording found in ' + OUTPUT_DIR);
  }
  const latestVideo = videos.sort().pop();
  const finalPath = path.join(OUTPUT_DIR, 'report-walkthrough.webm');
  fs.renameSync(path.join(OUTPUT_DIR, latestVideo), finalPath);
  console.log(`  Recording saved to: ${finalPath}`);
}

async function captureTimelineScreenshot(browser, port) {
  console.log('Capturing context timeline screenshot...');

  const context = await browser.newContext({
    viewport: SCREENSHOT_VIEWPORT,
    deviceScaleFactor: 2,
  });
  const page = await context.newPage();
  await openReport(page, port);

  const section = page.locator('.context-timeline-section');
  await section.scrollIntoViewIfNeeded();
  await page.waitForTimeout(500);
  await section.screenshot({ path: TIMELINE_SCREENSHOT });
  console.log(`  Screenshot saved to: ${TIMELINE_SCREENSHOT}`);

  await context.close();
}

function highlightJson(json) {
  const escaped = json
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
  return escaped
    .replace(/"([^"]+)":/g, '<span style="color:#7fd1ff">"$1"</span>:')
    .replace(/: "([^"]*)"/g, ': <span style="color:#a8e6a1">"$1"</span>')
    .replace(/: (-?\d+\.?\d*)/g, ': <span style="color:#f6c177">$1</span>')
    .replace(/: (null|true|false)/g, ': <span style="color:#c4a7e7">$1</span>');
}

async function captureResultsJsonScreenshot(browser) {
  console.log('Capturing results.json screenshot...');

  const resultsJsonPath = path.join(REPORT_DIR, 'results.json');
  const raw = fs.readFileSync(resultsJsonPath, 'utf-8');
  const pretty = JSON.stringify(JSON.parse(raw), null, 2);

  const context = await browser.newContext({
    viewport: { width: 900, height: 900 },
    deviceScaleFactor: 2,
  });
  const page = await context.newPage();
  await page.setContent(`
    <body style="margin: 0; padding: 40px; background: #f4f6f8; font-family: -apple-system, 'Segoe UI', Helvetica, Arial, sans-serif;">
      <div id="card" style="max-width: 780px; margin: 0 auto; border-radius: 12px; overflow: hidden; box-shadow: 0 12px 40px rgba(0,0,0,0.25);">
        <div style="background: #2c3e50; padding: 12px 18px; display: flex; align-items: center; gap: 8px;">
          <span style="width: 12px; height: 12px; border-radius: 50%; background: #ff5f57;"></span>
          <span style="width: 12px; height: 12px; border-radius: 50%; background: #febc2e;"></span>
          <span style="width: 12px; height: 12px; border-radius: 50%; background: #28c840;"></span>
          <span style="margin-left: 12px; color: #cfd8e3; font-size: 14px; font-family: ui-monospace, 'SF Mono', Menlo, monospace;">target/spring-test-profiler/results.json</span>
        </div>
        <pre style="margin: 0; padding: 24px 28px; background: #1e2430; color: #e6edf3; font-family: ui-monospace, 'SF Mono', Menlo, monospace; font-size: 15px; line-height: 1.55;">${highlightJson(pretty)}</pre>
      </div>
    </body>
  `);
  await page.waitForTimeout(300);
  await page.locator('#card').screenshot({ path: RESULTS_JSON_SCREENSHOT });
  console.log(`  Screenshot saved to: ${RESULTS_JSON_SCREENSHOT}`);

  await context.close();
}

async function main() {
  if (!fs.existsSync(path.join(REPORT_DIR, 'latest.html'))) {
    console.error(`Report not found at ${REPORT_DIR}. Run the demo tests first (see media/record-report-gif.sh).`);
    process.exit(1);
  }
  if (!fs.existsSync(path.join(REPORT_DIR, 'results.json'))) {
    console.error(`results.json not found at ${REPORT_DIR}. Run the demo tests first.`);
    process.exit(1);
  }

  fs.mkdirSync(OUTPUT_DIR, { recursive: true });
  fs.mkdirSync(ASSETS_DIR, { recursive: true });

  const server = await startServer();
  const port = server.address().port;
  console.log(`Local server on port ${port}`);

  const browser = await chromium.launch();

  if (!gifOnly) {
    await captureTimelineScreenshot(browser, port);
    await captureResultsJsonScreenshot(browser);
  }
  if (!screenshotsOnly) {
    await recordGif(browser, port);
  }

  await browser.close();
  server.close();
}

main().catch(err => {
  console.error('Recording failed:', err);
  process.exit(1);
});
