#!/usr/bin/env sh
set -e
set -x
./gradlew -p test-plugin build
./gradlew -p test-plugin-isolated build
./gradlew -p test-settings-plugin build
./gradlew -p test-app build