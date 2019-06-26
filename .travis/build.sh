#!/bin/bash

chmod u+x gradlew

echo "Compiling core JAR..."

./gradlew coreJar --no-daemon --stacktrace --console=plain

echo "Running tests..."

./gradlew test --no-daemon --info --console=plain --rerun-tasks

echo "Generating coverage report..."

./gradlew jacocoTestReport

echo "Uploading report to codecov.io..."

bash <(curl -s https://codecov.io/bash) -t $CODECOV_TOKEN

echo "Complete"

echo "real branch:"
echo $REAL_BRANCH