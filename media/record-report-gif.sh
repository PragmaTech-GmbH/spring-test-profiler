#!/bin/bash

# record-report-gif.sh - Record the report walkthrough GIF and marketing screenshots
# Builds the profiler, runs the demo tests to generate a fresh report, records a
# short walkthrough with Playwright, and converts it to an optimized GIF via ffmpeg.
#
# Usage:
#   ./media/record-report-gif.sh                 # full run (build + demo + record + convert)
#   ./media/record-report-gif.sh --skip-build    # reuse the existing demo report
#   ./media/record-report-gif.sh --screenshots-only  # only refresh the PNG screenshots
#   ./media/record-report-gif.sh --gif-only      # only re-record and convert the GIF

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
RESULTS_JSON="$DEMO_DIR/target/spring-test-profiler/results.json"
OUTPUT_DIR="$SCRIPT_DIR/output"
ASSETS_DIR="$PROJECT_DIR/docs/marketing/announcements/assets"
FINAL_GIF="$ASSETS_DIR/report-walkthrough.gif"

SKIP_BUILD=false
MJS_FLAGS=()
SCREENSHOTS_ONLY=false
for arg in "$@"; do
  case "$arg" in
    --skip-build) SKIP_BUILD=true ;;
    --screenshots-only) SCREENSHOTS_ONLY=true; MJS_FLAGS+=("--screenshots-only") ;;
    --gif-only) MJS_FLAGS+=("--gif-only") ;;
    *) echo -e "${RED}Unknown flag: $arg${NC}"; exit 1 ;;
  esac
done

echo -e "${BLUE}Spring Test Profiler - Report GIF Recorder${NC}"
echo -e "${BLUE}==========================================${NC}"
echo

echo -e "${YELLOW}Step 1: Checking prerequisites...${NC}"

for tool in node npx; do
  if ! command -v "$tool" > /dev/null 2>&1; then
    echo -e "${RED}$tool is required but not found${NC}"
    exit 1
  fi
done

if ! command -v ffmpeg > /dev/null 2>&1; then
  echo -e "${RED}ffmpeg is required but not found - install it with: brew install ffmpeg${NC}"
  exit 1
fi

echo -e "${GREEN}Prerequisites met${NC}"
echo

if [ "$SKIP_BUILD" = false ]; then
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
else
  echo -e "${YELLOW}Step 2: Skipping build (--skip-build), reusing existing report${NC}"
fi

if [ ! -f "$REPORT_FILE" ] || [ ! -f "$RESULTS_JSON" ]; then
  echo -e "${RED}Report or results.json not found in $DEMO_DIR/target/spring-test-profiler${NC}"
  exit 1
fi
echo

echo -e "${YELLOW}Step 3: Ensuring Playwright is available...${NC}"
cd "$PROJECT_DIR"
npm install --no-save playwright > /dev/null 2>&1
npx playwright install chromium > /dev/null 2>&1
echo -e "${GREEN}Playwright ready${NC}"
echo

echo -e "${YELLOW}Step 4: Recording...${NC}"
mkdir -p "$OUTPUT_DIR"
node "$SCRIPT_DIR/record-report-gif.mjs" "${MJS_FLAGS[@]}"
echo

if [ "$SCREENSHOTS_ONLY" = true ]; then
  echo -e "${GREEN}Screenshots refreshed:${NC}"
  ls -lh "$ASSETS_DIR"/*.png | awk '{print "  " $9 " (" $5 ")"}'
  exit 0
fi

echo -e "${YELLOW}Step 5: Converting to GIF (two-pass palette)...${NC}"

WEBM="$OUTPUT_DIR/report-walkthrough.webm"
PALETTE="$OUTPUT_DIR/palette.png"
RAW_GIF="$OUTPUT_DIR/report-walkthrough.gif"

# 8 fps, 800 px wide, 64 colors, no dithering: the report is a flat UI, so this
# keeps text crisp while staying well under the ~2 MB newsletter budget.
ffmpeg -y -loglevel error -i "$WEBM" \
  -vf "fps=8,scale=800:-1:flags=lanczos,palettegen=stats_mode=diff:max_colors=64" "$PALETTE"
ffmpeg -y -loglevel error -i "$WEBM" -i "$PALETTE" \
  -lavfi "fps=8,scale=800:-1:flags=lanczos[x];[x][1:v]paletteuse=dither=none:diff_mode=rectangle" \
  -loop 0 "$RAW_GIF"

mkdir -p "$ASSETS_DIR"
cp "$RAW_GIF" "$FINAL_GIF"

GIF_SIZE_BYTES=$(stat -f%z "$FINAL_GIF" 2>/dev/null || stat -c%s "$FINAL_GIF")
GIF_SIZE_HUMAN=$(du -h "$FINAL_GIF" | cut -f1)

echo -e "${GREEN}Done!${NC}"
echo -e "${BLUE}Outputs:${NC}"
echo -e "  GIF:        $FINAL_GIF ($GIF_SIZE_HUMAN)"
if [ -f "$ASSETS_DIR/context-timeline-screenshot.png" ]; then
  echo -e "  Timeline:   $ASSETS_DIR/context-timeline-screenshot.png ($(du -h "$ASSETS_DIR/context-timeline-screenshot.png" | cut -f1))"
fi
if [ -f "$ASSETS_DIR/results-json-screenshot.png" ]; then
  echo -e "  JSON:       $ASSETS_DIR/results-json-screenshot.png ($(du -h "$ASSETS_DIR/results-json-screenshot.png" | cut -f1))"
fi

if [ "$GIF_SIZE_BYTES" -gt 3000000 ]; then
  echo
  echo -e "${YELLOW}Warning: GIF is larger than 3 MB. Consider lowering fps=10 to fps=8 or scale=960 to scale=800 in this script.${NC}"
fi
