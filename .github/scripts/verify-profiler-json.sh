#!/usr/bin/env bash
# Verifies the Spring Test Profiler flat JSON summary report of a demo project.
#
# Each demo project can declare its expected metrics in a context-info.json file
# at the demo root, e.g. {"expectedContextsCreated": 7}. If the file is missing,
# verification is skipped so demos can opt in one by one.
#
# Usage: verify-profiler-json.sh <demo-dir> <build-output-dir (target|build)>
set -euo pipefail

demoDir="$1"
buildOutputDir="$2"

reportDir="$demoDir/$buildOutputDir/spring-test-profiler"
resultsFile="$reportDir/results.json"
expectationsFile="$demoDir/context-info.json"

if [ ! -f "$expectationsFile" ]; then
  echo "No context-info.json in $demoDir - skipping JSON summary verification"
  exit 0
fi

if [ ! -f "$resultsFile" ]; then
  echo "ERROR: expected JSON summary report at $resultsFile but it does not exist"
  exit 1
fi

if ! ls "$reportDir"/test-profiler-report-*.json > /dev/null 2>&1; then
  echo "ERROR: expected a timestamped test-profiler-report-*.json in $reportDir"
  exit 1
fi

echo "Verifying $resultsFile against $expectationsFile"

expectedContexts=$(jq -r '.expectedContextsCreated' "$expectationsFile")
actualContexts=$(jq -r '.contextsCreated' "$resultsFile")

if [ "$actualContexts" != "$expectedContexts" ]; then
  echo "ERROR: contextsCreated mismatch: expected $expectedContexts but was $actualContexts"
  echo "Full results.json content:"
  cat "$resultsFile"
  exit 1
fi

schemaVersion=$(jq -r '.schemaVersion' "$resultsFile")
totalDurationMs=$(jq -r '.totalDurationMs' "$resultsFile")

if [ "$schemaVersion" != "1" ]; then
  echo "ERROR: unexpected schemaVersion: $schemaVersion"
  exit 1
fi

if [ "$totalDurationMs" -le 0 ]; then
  echo "ERROR: totalDurationMs must be greater than 0 but was $totalDurationMs"
  exit 1
fi

echo "JSON summary verification passed: contextsCreated=$actualContexts, totalDurationMs=${totalDurationMs}ms"
