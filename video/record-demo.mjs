import { chromium } from 'playwright';
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const REPORT_DIR = path.resolve(__dirname, '../demo/spring-boot-4.0-maven/target/spring-test-profiler');
const SLIDES_FILE = path.resolve(__dirname, 'slides.html');
const OUTPUT_DIR = path.resolve(__dirname, 'output');

// Timing constants (milliseconds)
const SLIDE_1_DURATION = 15000;  // Title
const SLIDE_2_DURATION = 28000;  // Problem
const SLIDE_3_DURATION = 23000;  // Solution
const SLIDE_4_DURATION = 5000;   // Transition
const SLIDE_5_DURATION = 22000;  // CTA

const SCROLL_PAUSE = 4000;       // Pause after each scroll
const SECTION_PAUSE = 7000;      // Pause on key sections

// MIME types for the local server
const mimeTypes = {
  '.html': 'text/html',
  '.css': 'text/css',
  '.js': 'application/javascript',
  '.png': 'image/png',
  '.svg': 'image/svg+xml',
  '.woff2': 'font/woff2',
  '.woff': 'font/woff',
  '.ttf': 'font/ttf',
};

// Start a local HTTP server for the report
function startServer() {
  const server = http.createServer((req, res) => {
    let urlPath = req.url.split('?')[0];

    // Serve slides
    if (urlPath === '/slides.html') {
      fs.readFile(SLIDES_FILE, (err, data) => {
        if (err) { res.writeHead(404); res.end('Not found'); return; }
        res.writeHead(200, { 'Content-Type': 'text/html' });
        res.end(data);
      });
      return;
    }

    // Serve report files
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

// Smooth scroll helper
async function smoothScroll(page, pixels, duration = 1000) {
  await page.evaluate(([px, dur]) => {
    window.scrollBy({ top: px, behavior: 'smooth' });
  }, [pixels, duration]);
  await page.waitForTimeout(duration + 500);
}

// Scroll to element with highlight
async function scrollToAndHighlight(page, selector, pause = SECTION_PAUSE) {
  const element = await page.$(selector);
  if (element) {
    await element.scrollIntoViewIfNeeded();
    await page.waitForTimeout(800);

    // Add highlight effect
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

    await page.waitForTimeout(pause);

    // Remove highlight
    await page.evaluate((sel) => {
      const el = document.querySelector(sel);
      if (el) {
        el.style.boxShadow = '';
        el.style.outline = '';
        el.style.outlineOffset = '';
      }
    }, selector);
    await page.waitForTimeout(500);
  }
}

async function showSlide(page, slideNumber) {
  await page.evaluate((num) => {
    document.querySelectorAll('.slide').forEach(s => s.classList.remove('active'));
    const slide = document.getElementById(`slide-${num}`);
    if (slide) slide.classList.add('active');
  }, slideNumber);
  await page.waitForTimeout(300);
}

async function main() {
  console.log('Starting Spring Test Profiler demo recording...');

  // Verify report exists
  if (!fs.existsSync(path.join(REPORT_DIR, 'latest.html'))) {
    console.error('Report not found. Run the demo tests first.');
    process.exit(1);
  }

  const server = await startServer();
  const port = server.address().port;
  console.log(`Local server on port ${port}`);

  // Launch browser with video recording
  const browser = await chromium.launch();
  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 },
    recordVideo: {
      dir: OUTPUT_DIR,
      size: { width: 1920, height: 1080 },
    },
  });

  const page = await context.newPage();

  // ============================================
  // PART 1: SLIDES
  // ============================================
  console.log('Recording slides...');

  await page.goto(`http://localhost:${port}/slides.html`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);

  // Slide 1: Title
  console.log('  Slide 1: Title');
  await page.waitForTimeout(SLIDE_1_DURATION);

  // Slide 2: The Problem
  console.log('  Slide 2: The Problem');
  await showSlide(page, 2);
  await page.waitForTimeout(SLIDE_2_DURATION);

  // Slide 3: The Solution
  console.log('  Slide 3: Zero-Config Profiling');
  await showSlide(page, 3);
  await page.waitForTimeout(SLIDE_3_DURATION);

  // Slide 4: Transition
  console.log('  Slide 4: Transition');
  await showSlide(page, 4);
  await page.waitForTimeout(SLIDE_4_DURATION);

  // ============================================
  // PART 2: REPORT WALKTHROUGH
  // ============================================
  console.log('Recording report walkthrough...');

  await page.goto(`http://localhost:${port}/latest.html`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(2000); // Let D3 charts render

  // Inject smooth scroll CSS
  await page.addStyleTag({
    content: `html { scroll-behavior: smooth; }`
  });

  // 1. Report header + Summary cards
  console.log('  Section: Summary');
  await page.waitForTimeout(SCROLL_PAUSE);

  // Scroll to summary grid and highlight it
  await scrollToAndHighlight(page, '.summary-grid', SECTION_PAUSE);

  // 2. Context Cache Entries
  console.log('  Section: Cache Entries');
  await scrollToAndHighlight(page, '.cache-stats', SCROLL_PAUSE);

  // Highlight sort controls briefly
  await scrollToAndHighlight(page, '.sort-controls', 3000);

  // Highlight individual cache entries
  const cacheEntries = await page.$$('.cache-entry');
  for (let i = 0; i < Math.min(cacheEntries.length, 3); i++) {
    console.log(`  Highlighting cache entry ${i + 1}`);
    await cacheEntries[i].scrollIntoViewIfNeeded();
    await page.waitForTimeout(800);

    // Highlight this entry
    await page.evaluate((idx) => {
      const entries = document.querySelectorAll('.cache-entry');
      if (entries[idx]) {
        entries[idx].style.transition = 'box-shadow 0.5s ease';
        entries[idx].style.boxShadow = '0 0 0 3px rgba(109, 179, 63, 0.4), 0 0 20px rgba(109, 179, 63, 0.15)';
        entries[idx].style.borderRadius = '8px';
      }
    }, i);

    await page.waitForTimeout(SECTION_PAUSE);

    await page.evaluate((idx) => {
      const entries = document.querySelectorAll('.cache-entry');
      if (entries[idx]) {
        entries[idx].style.boxShadow = '';
      }
    }, i);
    await page.waitForTimeout(500);
  }

  // 3. Context Comparison
  console.log('  Section: Context Comparison');
  const comparisonSection = await page.$('.context-comparison');
  if (comparisonSection) {
    await comparisonSection.scrollIntoViewIfNeeded();
    await page.waitForTimeout(1500);

    // Try to click the first comparison pair to trigger the diff view
    const compareButtons = await page.$$('.compare-btn, .context-select, [data-context-id]');
    if (compareButtons.length >= 2) {
      await compareButtons[0].click();
      await page.waitForTimeout(1000);
      await compareButtons[1].click();
      await page.waitForTimeout(1000);
    }

    // Highlight the diff area if visible
    const diffView = await page.$('.diff-output, .comparison-result, .diff-container');
    if (diffView) {
      await scrollToAndHighlight(page, '.diff-output, .comparison-result, .diff-container', SECTION_PAUSE);
    } else {
      await page.waitForTimeout(SECTION_PAUSE);
    }
  }

  // 4. Quick scroll through configurations
  console.log('  Section: Configurations (quick scroll)');
  await smoothScroll(page, 800, 2000);
  await page.waitForTimeout(SCROLL_PAUSE);
  await smoothScroll(page, 600, 2000);
  await page.waitForTimeout(4000);

  // ============================================
  // PART 3: CLOSING SLIDE
  // ============================================
  console.log('Recording closing slide...');

  await page.goto(`http://localhost:${port}/slides.html`, { waitUntil: 'networkidle' });
  await showSlide(page, 5);
  await page.waitForTimeout(SLIDE_5_DURATION);

  // ============================================
  // CLEANUP
  // ============================================
  console.log('Finishing recording...');

  await page.close();
  const videoPath = await (await context.pages()[0]?.video()?.path?.() ?? page.video()?.path());

  await context.close();
  await browser.close();
  server.close();

  // Find the output video
  const videos = fs.readdirSync(OUTPUT_DIR).filter(f => f.endsWith('.webm'));
  if (videos.length > 0) {
    const latestVideo = videos.sort().pop();
    const finalPath = path.join(OUTPUT_DIR, 'demo.webm');
    fs.renameSync(path.join(OUTPUT_DIR, latestVideo), finalPath);
    console.log(`\nVideo saved to: ${finalPath}`);
  } else {
    console.log('\nVideo files in output:', fs.readdirSync(OUTPUT_DIR));
  }
}

main().catch(err => {
  console.error('Recording failed:', err);
  process.exit(1);
});
