#!/bin/bash

# renderReport.sh - Automated Spring Test Profiler Report Generation
# This script builds the profiler, runs demo tests, and opens the generated report

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEMO_DIR="$SCRIPT_DIR/demo/spring-boot-3.5-maven"
REPORT_PATH="$DEMO_DIR/target/spring-test-profiler/latest.html"

echo -e "${BLUE}🚀 Spring Test Profiler Report Generator${NC}"
echo -e "${BLUE}=======================================${NC}"
echo

# Step 1: Build and install the profiler
echo -e "${YELLOW}📦 Step 1: Building and installing Spring Test Profiler...${NC}"
cd "$SCRIPT_DIR"

if ./mvnw clean spotless:apply install -DskipTests -q; then
    echo -e "${GREEN}✅ Profiler built and installed successfully${NC}"
else
    echo -e "${RED}❌ Failed to build and install profiler${NC}"
    exit 1
fi

echo

# Step 2: Run demo tests
echo -e "${YELLOW}🧪 Step 2: Running demo tests to generate report...${NC}"
cd "$DEMO_DIR"

if mvn test -Dtest="*IT" -U -q; then
    echo -e "${GREEN}✅ Demo tests completed successfully${NC}"
else
    echo -e "${RED}❌ Demo tests failed${NC}"
    exit 1
fi

echo

# Step 3: Check if report was generated
echo -e "${YELLOW}📊 Step 3: Checking for generated report...${NC}"

if [ -f "$REPORT_PATH" ]; then
    echo -e "${GREEN}✅ Report generated at: $REPORT_PATH${NC}"
else
    echo -e "${RED}❌ Report not found at expected location${NC}"
    echo -e "${RED}   Expected: $REPORT_PATH${NC}"
    exit 1
fi

echo

# Step 4: Wait and open report in browser
echo -e "${YELLOW}🌐 Step 4: Opening report in browser...${NC}"
echo -e "${BLUE}Waiting 5 seconds before opening browser...${NC}"

for i in {5..1}; do
    echo -ne "${BLUE}Opening in ${i} seconds...\r${NC}"
    sleep 1
done

echo -e "\n${GREEN}🎉 Opening Spring Test Profiler Report!${NC}"

# Open in default browser (works on macOS, Linux, and Windows)
if command -v open > /dev/null 2>&1; then
    # macOS
    open "$REPORT_PATH"
elif command -v xdg-open > /dev/null 2>&1; then
    # Linux
    xdg-open "$REPORT_PATH"
elif command -v start > /dev/null 2>&1; then
    # Windows
    start "$REPORT_PATH"
else
    echo -e "${YELLOW}⚠️  Could not detect browser opener command${NC}"
    echo -e "${BLUE}Please manually open: $REPORT_PATH${NC}"
fi

echo
echo -e "${GREEN}✨ Report generation complete!${NC}"
echo -e "${BLUE}📁 Report location: $REPORT_PATH${NC}"
echo -e "${BLUE}🔍 The report shows Spring context usage, caching statistics, and optimization opportunities${NC}"
echo
