#!/bin/bash

# Script to update versions after a successful release
# Usage: ./update-versions.sh <releaseVersion> <nextDevVersion>

set -e

if [ $# -ne 2 ]; then
    echo "Usage: $0 <releaseVersion> <nextDevVersion>"
    echo "Example: $0 1.0.0 1.1.0-SNAPSHOT"
    exit 1
fi

RELEASE_VERSION="$1"
NEXT_DEV_VERSION="$2"

echo "🔄 Updating versions..."
echo "Release version: $RELEASE_VERSION"
echo "Next dev version: $NEXT_DEV_VERSION"

# Update README.md and wiki docs with release version
echo "📝 Updating README.md and docs/wiki with release version..."
# Use cross-platform sed approach
for docFile in README.md docs/wiki/Getting-Started.md; do
  if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    sed -i "" "s/Latest%20Version-[0-9]*\.[0-9]*\.[0-9]*/Latest%20Version-$RELEASE_VERSION/" "$docFile"
    sed -i "" "s/<version>[0-9]*\.[0-9]*\.[0-9]*<\/version>/<version>$RELEASE_VERSION<\/version>/" "$docFile"
    sed -i "" "s/spring-test-profiler:[0-9]*\.[0-9]*\.[0-9]*/spring-test-profiler:$RELEASE_VERSION/" "$docFile"
  else
    # Linux
    sed -i "s/Latest%20Version-[0-9]*\.[0-9]*\.[0-9]*/Latest%20Version-$RELEASE_VERSION/" "$docFile"
    sed -i "s/<version>[0-9]*\.[0-9]*\.[0-9]*<\/version>/<version>$RELEASE_VERSION<\/version>/" "$docFile"
    sed -i "s/spring-test-profiler:[0-9]*\.[0-9]*\.[0-9]*/spring-test-profiler:$RELEASE_VERSION/" "$docFile"
  fi
  echo "✅ Updated $docFile with release version $RELEASE_VERSION"
done

# Set next development version in main pom.xml
echo "📝 Updating main pom.xml to next development version..."
./mvnw --batch-mode versions:set -DnewVersion="$NEXT_DEV_VERSION"
echo "✅ Updated main pom.xml to $NEXT_DEV_VERSION"

# Update demo Maven projects (only spring-test-profiler dependency)
echo "📝 Updating demo Maven projects..."
for pom in $(find demo -name "pom.xml"); do
  # Use perl for multiline matching to target only spring-test-profiler dependency
  perl -i -pe 'BEGIN{undef $/;} s|(<groupId>digital\.pragmatech\.testing</groupId>\s*<artifactId>spring-test-profiler</artifactId>\s*)<version>[0-9]*\.[0-9]*\.[0-9]*(-SNAPSHOT)?</version>|${1}<version>'"$NEXT_DEV_VERSION"'</version>|smg' "$pom"
done
echo "✅ Updated demo Maven projects spring-test-profiler dependency to $NEXT_DEV_VERSION"

# Update demo Gradle projects
echo "📝 Updating demo Gradle projects..."
if [[ "$OSTYPE" == "darwin"* ]]; then
  # macOS
  find demo -name "build.gradle" -exec sed -i "" "s/digital.pragmatech.testing:spring-test-profiler:[^']*/digital.pragmatech.testing:spring-test-profiler:$NEXT_DEV_VERSION/" {} \;
else
  # Linux
  find demo -name "build.gradle" -exec sed -i "s/digital.pragmatech.testing:spring-test-profiler:[^']*/digital.pragmatech.testing:spring-test-profiler:$NEXT_DEV_VERSION/" {} \;
fi
echo "✅ Updated demo Gradle projects to $NEXT_DEV_VERSION"

echo "🎉 All version updates completed successfully!"
