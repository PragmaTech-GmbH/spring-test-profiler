#!/usr/bin/env bash

echo "Building spring-boot-3.5-gradle demo..."

# Clear previous environment file
rm -f build-env.properties

# Export the report paths for CI (do this first so it's always available)
echo "SPRING_TEST_INSIGHT_REPORTS=build/spring-test-insight" >> build-env.properties
echo "TEST_REPORTS=build/reports/tests" >> build-env.properties
echo "TEST_RESULTS=build/test-results" >> build-env.properties

# Run Gradle build
./gradlew clean test
BUILD_STATUS=$?

if [ $BUILD_STATUS -eq 0 ]; then
    echo "Build completed successfully!"
else
    echo "Build failed with status: $BUILD_STATUS"
fi

echo "Spring Test Insight reports available at: build/spring-test-insight"
echo "Test reports available at: build/reports/tests"
echo "Test results available at: build/test-results"

exit $BUILD_STATUS