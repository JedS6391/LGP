#!/bin/bash

branch_name=$(echo $1 | sed -e 's/\//-/g')
tag_name=$2
now=$(date +%Y-%m-%d)
version=$(gradle -q printVersion)
release_branch_regex="^release.*$"
feature_branch_regex="^feature.*$"

echo "$branch_name"
echo "$tag_name"

cd ../

if [[ "$branch_name" =~ $feature_branch_regex ]]; then
    echo "Renaming build files for feature branch..."
    rm build/libs/LGP-"$version".jar
    mv build/libs/LGP-core-"$version".jar build/libs/LGP-core-"$branch_name"-"$now".jar
    mv build/libs/LGP-examples-"$version".jar build/libs/LGP-examples-"$branch_name"-"$now".jar
    ls build/libs
elif [[ "$branch_name" =~ $release_branch_regex ]]; then
    echo "Renaming build files for release branch..."
    rm build/libs/LGP-"$version".jar
    mv build/libs/LGP-core-"$version".jar build/libs/LGP-core-"$tag_name"-"$now".jar
    mv build/libs/LGP-examples-"$version".jar build/libs/LGP-examples-"$tag_name"-"$now".jar
    ls build/libs
else
    echo "Invalid deployment type"
fi


if [ "$TRAVIS_BRANCH" = "master" ] && [ "$TRAVIS_PULL_REQUEST" = "false" ] && [ $TRAVIS_TEST_RESULT -eq 0 ]
then
	echo "Signing POM, upload archives to staging repository"
	./gradlew uploadArchives
fi
