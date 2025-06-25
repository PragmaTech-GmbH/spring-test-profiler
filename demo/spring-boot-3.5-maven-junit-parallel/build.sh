#!/usr/bin/env bash

# Exit on error
set -e

echo "Building spring-boot-3.5-maven-junit-parallel demo..."

# Run Maven build with JUnit parallel execution
./mvnw clean verify

# Export the report paths for CI
echo "SPRING_TEST_INSIGHT_REPORTS=target/spring-test-insight" >> build-env.properties
echo "SUREFIRE_REPORTS=target/surefire-reports" >> build-env.properties
echo "FAILSAFE_REPORTS=target/failsafe-reports" >> build-env.properties

echo "Build completed successfully!"
echo "Spring Test Insight reports available at: $SPRING_TEST_INSIGHT_REPORTS"
echo "Surefire reports available at: $SUREFIRE_REPORTS"
echo "Failsafe reports available at: $FAILSAFE_REPORTS"