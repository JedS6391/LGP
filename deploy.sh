#!/bin/bash

deployment_type=$1
branch_name=$2
tag_name=$3
now=$(date +%Y-%m-%d)
version=$(gradle -q printVersion)

case "$deployment_type" in
"feature")
    echo "Renaming build files for feature branch..."
    rm build/libs/LGP-"$version".jar
    mv build/libs/LGP-core-"$version".jar build/libs/LGP-core-"$branch_name"-"$now".jar
    mv build/libs/LGP-examples-"$version".jar build/libs/LGP-examples-"$branch_name"-"$now".jar
    ls build/libs
    ;;
"release")
    echo "Renaming build files for release branch..."
    rm build/libs/LGP-"$version".jar
    mv build/libs/LGP-core-"$version".jar build/libs/LGP-core-"$tag_name"-"$now".jar
    mv build/libs/LGP-examples-"$version".jar build/libs/LGP-examples-"$tag_name"-"$now".jar
    ls build/libs
    ;;
*)
    echo "Invalid deployment type"
    ;;
esac
