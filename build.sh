#!/bin/bash

# Central build script for all demo projects
# This script builds and tests all Spring Test Insight demo projects

set -e  # Exit on first error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to build a demo project
build_demo() {
    local demo_path="$1"
    local demo_name="$(basename "$demo_path")"
    local build_command="$2"

    print_status "Building demo: $demo_name"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    cd "$demo_path"

    if eval "$build_command"; then
        print_success "$demo_name build completed successfully"
    else
        print_error "$demo_name build failed"
        exit 1
    fi

    echo
    cd - > /dev/null
}

# Get the script directory (where this script is located)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEMO_DIR="$SCRIPT_DIR/demo"

print_status "Starting build for all Spring Test Insight demo projects"
print_status "Demo directory: $DEMO_DIR"
echo

# Check if demo directory exists
if [ ! -d "$DEMO_DIR" ]; then
    print_error "Demo directory not found: $DEMO_DIR"
    exit 1
fi

# Build extension first
print_status "Building Spring Test Insight Extension"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
cd "$SCRIPT_DIR/spring-test-profiler-extension"
if ./mvnw install -q; then
    print_success "Extension built and installed to local repository"
else
    print_error "Extension build failed"
    exit 1
fi
echo
cd - > /dev/null

# Demo projects and their build commands
DEMOS=(
    "spring-boot-3.4-maven:./mvnw verify"
    "spring-boot-3.5-maven:./mvnw verify"
    "spring-boot-3.5-gradle:./gradlew build"
    "spring-boot-3.5-maven-failsafe-parallel:./mvnw verify"
    "spring-boot-3.5-maven-junit-parallel:./mvnw verify"
)

# Track results
SUCCESSFUL_BUILDS=()
FAILED_BUILDS=()

# Build each demo
for demo_entry in "${DEMOS[@]}"; do
    demo="${demo_entry%%:*}"
    build_command="${demo_entry#*:}"
    demo_path="$DEMO_DIR/$demo"

    if [ ! -d "$demo_path" ]; then
        print_warning "Demo directory not found: $demo_path (skipping)"
        continue
    fi

    if build_demo "$demo_path" "$build_command"; then
        SUCCESSFUL_BUILDS+=("$demo")
    else
        FAILED_BUILDS+=("$demo")
    fi
done

# Print summary
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
print_status "BUILD SUMMARY"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ ${#SUCCESSFUL_BUILDS[@]} -gt 0 ]; then
    print_success "Successful builds (${#SUCCESSFUL_BUILDS[@]}):"
    for demo in "${SUCCESSFUL_BUILDS[@]}"; do
        echo "  ✅ $demo"
    done
    echo
fi

if [ ${#FAILED_BUILDS[@]} -gt 0 ]; then
    print_error "Failed builds (${#FAILED_BUILDS[@]}):"
    for demo in "${FAILED_BUILDS[@]}"; do
        echo "  ❌ $demo"
    done
    echo
    exit 1
else
    print_success "All demo projects built successfully! 🎉"
    print_status "Spring Test Insight reports have been generated in each demo's target/spring-test-profiler/ directory"
fi
