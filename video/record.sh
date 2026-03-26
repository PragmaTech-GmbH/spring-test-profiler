#!/bin/bash

# record.sh - Record the Spring Test Profiler YouTube demo video
# Builds the profiler, runs demo tests to generate a fresh report,
# then records a ~3 minute video using Playwright.

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
DEMO_DIR="$PROJECT_DIR/demo/spring-boot-4.0-maven"
REPORT_FILE="$DEMO_DIR/target/spring-test-profiler/latest.html"
OUTPUT_DIR="$SCRIPT_DIR/output"

echo -e "${BLUE}🎬 Spring Test Profiler - Demo Video Recorder${NC}"
echo -e "${BLUE}================================================${NC}"
echo

# Step 1: Check prerequisites
echo -e "${YELLOW}Step 1: Checking prerequisites...${NC}"

if ! command -v node > /dev/null 2>&1; then
    echo -e "${RED}Node.js is required but not found${NC}"
    exit 1
fi

if ! command -v npx > /dev/null 2>&1; then
    echo -e "${RED}npx is required but not found${NC}"
    exit 1
fi

echo -e "${GREEN}Prerequisites met${NC}"
echo

# Step 2: Build profiler and run demo tests
echo -e "${YELLOW}Step 2: Building profiler and running demo tests...${NC}"

cd "$PROJECT_DIR"
if ./mvnw clean install -DskipTests -q; then
    echo -e "${GREEN}Profiler built${NC}"
else
    echo -e "${RED}Failed to build profiler${NC}"
    exit 1
fi

cd "$DEMO_DIR"
if ./mvnw clean verify -q; then
    echo -e "${GREEN}Demo tests completed, report generated${NC}"
else
    echo -e "${RED}Demo tests failed${NC}"
    exit 1
fi

if [ ! -f "$REPORT_FILE" ]; then
    echo -e "${RED}Report not found at: $REPORT_FILE${NC}"
    exit 1
fi

echo

# Step 3: Install Playwright
echo -e "${YELLOW}Step 3: Ensuring Playwright is available...${NC}"
cd "$PROJECT_DIR"
npm install --no-save playwright > /dev/null 2>&1
npx playwright install chromium > /dev/null 2>&1
echo -e "${GREEN}Playwright ready${NC}"
echo

# Step 4: Record video
echo -e "${YELLOW}Step 4: Recording demo video...${NC}"
echo -e "${BLUE}This will take approximately 3 minutes.${NC}"
echo

mkdir -p "$OUTPUT_DIR"

cd "$PROJECT_DIR"
node "$SCRIPT_DIR/record-demo.mjs"

echo
if [ -f "$OUTPUT_DIR/demo.webm" ]; then
    SIZE=$(du -h "$OUTPUT_DIR/demo.webm" | cut -f1)
    echo -e "${GREEN}Recording complete!${NC}"
    echo -e "${BLUE}Output: $OUTPUT_DIR/demo.webm ($SIZE)${NC}"
    echo
    echo -e "${YELLOW}Next steps:${NC}"
    echo -e "  1. Review the video: open $OUTPUT_DIR/demo.webm"
    echo -e "  2. Record your voiceover matching the slide timing"
    echo -e "  3. Combine video + audio in your video editor"
    echo -e "  4. Upload to YouTube"
else
    echo -e "${RED}Recording failed - no output video found${NC}"
    ls -la "$OUTPUT_DIR/"
    exit 1
fi
