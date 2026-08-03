#!/usr/bin/env bash
# Runs the same visual comparison as .github/workflows/visual-regression.yml locally:
# demo report built against the latest Maven Central release vs. the local working tree.
#
# Usage: ./visual-regression/run-local.sh [baseline-version]
#   baseline-version defaults to the <release> version from Maven Central metadata.

set -euo pipefail

scriptDir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repoRoot="$(cd "$scriptDir/.." && pwd)"
demoDir="$repoRoot/demo/spring-boot-4.0-maven"
workDir="$scriptDir/work"

restoreDemoPom() {
  git -C "$repoRoot" checkout -- "$demoDir/pom.xml" 2>/dev/null || true
}
trap restoreDemoPom EXIT

baselineVersion="${1:-}"
if [ -z "$baselineVersion" ]; then
  metadataUrl="https://repo1.maven.org/maven2/digital/pragmatech/testing/spring-test-profiler/maven-metadata.xml"
  baselineVersion="$(curl -sf "$metadataUrl" | sed -n 's/.*<release>\(.*\)<\/release>.*/\1/p' | head -1)"
fi
if [ -z "$baselineVersion" ]; then
  echo "Could not determine the latest released version from Maven Central" >&2
  exit 1
fi

currentVersion="$(cd "$repoRoot" && ./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)"
echo "Baseline: $baselineVersion (Maven Central), current: $currentVersion (local build)"

mkdir -p "$workDir"

echo "==> Running demo with released profiler $baselineVersion"
(
  cd "$demoDir"
  ./mvnw -q versions:use-dep-version \
    -Dincludes=digital.pragmatech.testing:spring-test-profiler \
    -DdepVersion="$baselineVersion" -DforceVersion=true -DgenerateBackupPoms=false
  # The released profiler may fail the demo's tests (e.g. 0.2.3 with the Modulith demo,
  # issue #58); the report is still generated, so keep the baseline leg alive.
  ./mvnw -q clean verify -Dmaven.test.failure.ignore=true
)
cp "$demoDir/target/spring-test-profiler/latest.html" "$workDir/baseline.html"
restoreDemoPom

echo "==> Building local profiler $currentVersion"
(cd "$repoRoot" && ./mvnw -q clean install -DskipTests)

echo "==> Running demo with local profiler $currentVersion"
(cd "$demoDir" && ./mvnw -q clean verify)
cp "$demoDir/target/spring-test-profiler/latest.html" "$workDir/current.html"

echo "==> Comparing reports"
cd "$scriptDir"
npm install
npx playwright install chromium
node compare-reports.mjs \
  --baseline work/baseline.html \
  --current work/current.html \
  --out work/out \
  --baseline-version "$baselineVersion" \
  --current-version "$currentVersion"

cat "$workDir/out/summary.json"
if [ "$(uname)" = "Darwin" ]; then
  open "$workDir/out/diff.png"
fi
