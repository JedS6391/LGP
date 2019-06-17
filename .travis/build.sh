#!/usr/bin/env sh

chmod u+x gradlew

echo "Compiling core JAR..."

./gradlew coreJar --no-daemon --stacktrace --console=plain

echo "Running tests..."

./gradlew test --no-daemon --info --console=plain

echo "real branch:"
echo $REAL_BRANCH