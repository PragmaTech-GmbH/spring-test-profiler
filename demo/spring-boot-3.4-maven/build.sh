#!/usr/bin/env bash

echo "Building spring-boot-3.4-maven demo..."

# Clear previous environment file
rm -f build-env.properties

# Export the report paths for CI (do this first so it's always available)
echo "SPRING_TEST_INSIGHT_REPORTS=target/spring-test-insight" >> build-env.properties
echo "SUREFIRE_REPORTS=target/surefire-reports" >> build-env.properties
echo "FAILSAFE_REPORTS=target/failsafe-reports" >> build-env.properties

# Run Maven build
./mvnw clean verify
BUILD_STATUS=$?

if [ $BUILD_STATUS -eq 0 ]; then
    echo "Build completed successfully!"
else
    echo "Build failed with status: $BUILD_STATUS"
fi

echo "Spring Test Insight reports available at: target/spring-test-insight"
echo "Surefire reports available at: target/surefire-reports"
echo "Failsafe reports available at: target/failsafe-reports"

exit $BUILD_STATUS