#!/bin/bash

branch_name=$(echo $1 | sed -e 's/\//-/g')
tag_name=$2
now=$(date +%Y-%m-%d)
version=$(./gradlew -q -Dorg.gradle.internal.launcher.welcomeMessageEnabled=false printVersion)
release_branch_regex="^release.*$"
feature_branch_regex="^feature.*$"

echo "$branch_name"
echo "$tag_name"

if [[ "$branch_name" =~ $feature_branch_regex ]]; then
    echo "Renaming build files for feature branch..."
    rm build/libs/LGP-"$version".jar
    mv build/libs/LGP-core-"$version".jar build/libs/LGP-core-"$branch_name"-"$now".jar
    ls build/libs
elif [[ "$branch_name" =~ $release_branch_regex ]]; then
    echo "Renaming build files for release branch..."
    rm build/libs/LGP-"$version".jar
    mv build/libs/LGP-core-"$version".jar build/libs/LGP-core-"$tag_name"-"$now".jar
    ls build/libs
else
    echo "Invalid deployment type"
fi