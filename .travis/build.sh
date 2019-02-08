#!/usr/bin/env sh

chmod u+x gradlew

./gradlew coreJar --no-daemon --stacktrace --console=plain
./gradlew examplesJar --no-daemon --stacktrace --console=plain