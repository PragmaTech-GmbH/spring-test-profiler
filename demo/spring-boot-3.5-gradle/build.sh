#!/usr/bin/env bash

# Exit on error
set -e

echo "Building spring-boot-3.5-gradle demo..."

# Run Gradle build
./gradlew clean test

# Export the report paths for CI
echo "SPRING_TEST_INSIGHT_REPORTS=build/spring-test-insight" >> build-env.properties
echo "TEST_REPORTS=build/reports/tests" >> build-env.properties
echo "TEST_RESULTS=build/test-results" >> build-env.properties

echo "Build completed successfully!"
echo "Spring Test Insight reports available at: $SPRING_TEST_INSIGHT_REPORTS"
echo "Test reports available at: $TEST_REPORTS"
echo "Test results available at: $TEST_RESULTS"