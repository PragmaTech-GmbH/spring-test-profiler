#!/bin/bash

# screenshotReport.sh - Capture full-page screenshot of the Spring Test Profiler report
# and split it into two halves for side-by-side display in the README

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPORT_DIR="$SCRIPT_DIR/demo/spring-boot-4.0-maven/target/spring-test-profiler"
REPORT_FILE="$REPORT_DIR/latest.html"
DOCS_DIR="$SCRIPT_DIR/docs"
FULL_SCREENSHOT="$DOCS_DIR/report-full.png"
TOP_SCREENSHOT="$DOCS_DIR/report-top.png"
BOTTOM_SCREENSHOT="$DOCS_DIR/report-bottom.png"

echo -e "${BLUE}📸 Spring Test Profiler Report Screenshot${NC}"
echo -e "${BLUE}==========================================${NC}"
echo

# Step 1: Check prerequisites
echo -e "${YELLOW}🔍 Step 1: Checking prerequisites...${NC}"

if ! command -v node > /dev/null 2>&1; then
    echo -e "${RED}❌ Node.js is required but not found${NC}"
    exit 1
fi

if ! command -v npx > /dev/null 2>&1; then
    echo -e "${RED}❌ npx is required but not found${NC}"
    exit 1
fi

if ! command -v magick > /dev/null 2>&1; then
    echo -e "${RED}❌ ImageMagick (magick) is required but not found${NC}"
    echo -e "${YELLOW}   Install via: brew install imagemagick${NC}"
    exit 1
fi

echo -e "${GREEN}✅ All prerequisites met${NC}"
echo

# Step 2: Build profiler and run demo tests to generate a fresh report
echo -e "${YELLOW}🧪 Step 2: Building profiler and running demo tests...${NC}"

cd "$SCRIPT_DIR"
if ./mvnw clean install -DskipTests -q; then
    echo -e "${GREEN}✅ Profiler built and installed${NC}"
else
    echo -e "${RED}❌ Failed to build profiler${NC}"
    exit 1
fi

DEMO_DIR="$SCRIPT_DIR/demo/spring-boot-4.0-maven"
cd "$DEMO_DIR"
if ./mvnw clean verify -q; then
    echo -e "${GREEN}✅ Demo tests completed, report generated${NC}"
else
    echo -e "${RED}❌ Demo tests failed${NC}"
    exit 1
fi

if [ ! -f "$REPORT_FILE" ]; then
    echo -e "${RED}❌ Report not found at: $REPORT_FILE${NC}"
    exit 1
fi

echo
cd "$SCRIPT_DIR"

# Step 3: Install Playwright
echo -e "${YELLOW}📦 Step 3: Installing Playwright...${NC}"
npm install --no-save playwright > /dev/null 2>&1
npx playwright install chromium > /dev/null 2>&1
echo -e "${GREEN}✅ Playwright ready${NC}"
echo

# Step 4: Take full-page screenshot
echo -e "${YELLOW}📸 Step 4: Capturing full-page screenshot...${NC}"

SCREENSHOT_SCRIPT="$SCRIPT_DIR/.screenshot-capture.mjs"
cat > "$SCREENSHOT_SCRIPT" << 'NODESCRIPT'
import { chromium } from 'playwright';
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';

const reportDir = process.argv[2];
const outputPath = process.argv[3];

const mimeTypes = {
  '.html': 'text/html',
  '.css': 'text/css',
  '.js': 'application/javascript',
  '.png': 'image/png',
  '.svg': 'image/svg+xml',
};

const server = http.createServer((req, res) => {
  const urlPath = req.url === '/' ? '/latest.html' : req.url.split('?')[0];
  const filePath = path.join(reportDir, urlPath);
  const ext = path.extname(filePath);

  fs.readFile(filePath, (err, data) => {
    if (err) {
      res.writeHead(404);
      res.end('Not found');
      return;
    }
    res.writeHead(200, { 'Content-Type': mimeTypes[ext] || 'application/octet-stream' });
    res.end(data);
  });
});

await new Promise(resolve => server.listen(0, resolve));
const port = server.address().port;

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1280, height: 800 } });
await page.goto(`http://localhost:${port}/latest.html`, { waitUntil: 'networkidle' });

// Wait for D3 chart animations to complete
await page.waitForTimeout(2000);

await page.screenshot({ path: outputPath, fullPage: true });

await browser.close();
server.close();
NODESCRIPT

node "$SCREENSHOT_SCRIPT" "$REPORT_DIR" "$FULL_SCREENSHOT"
rm -f "$SCREENSHOT_SCRIPT"

if [ ! -f "$FULL_SCREENSHOT" ]; then
    echo -e "${RED}❌ Screenshot capture failed${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Full-page screenshot captured${NC}"
echo

# Step 5: Split screenshot into top and bottom halves
echo -e "${YELLOW}✂️  Step 5: Splitting screenshot into two halves...${NC}"

HEIGHT=$(magick identify -format '%h' "$FULL_SCREENSHOT")
HALF_HEIGHT=$((HEIGHT / 2))

magick "$FULL_SCREENSHOT" -crop "x${HALF_HEIGHT}+0+0" +repage "$TOP_SCREENSHOT"
magick "$FULL_SCREENSHOT" -gravity South -crop "x${HALF_HEIGHT}+0+0" +repage "$BOTTOM_SCREENSHOT"

echo -e "${GREEN}✅ Screenshots split successfully${NC}"
echo -e "${BLUE}   Full:   $FULL_SCREENSHOT (${HEIGHT}px tall)${NC}"
echo -e "${BLUE}   Top:    $TOP_SCREENSHOT (${HALF_HEIGHT}px tall)${NC}"
echo -e "${BLUE}   Bottom: $BOTTOM_SCREENSHOT (${HALF_HEIGHT}px tall)${NC}"
echo

echo -e "${GREEN}✨ Screenshot generation complete!${NC}"
echo -e "${BLUE}📁 Screenshots saved to: $DOCS_DIR${NC}"
echo -e "${BLUE}📝 These are referenced in README.md as a side-by-side layout${NC}"
echo
