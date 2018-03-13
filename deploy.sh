#!/bin/bash

deployment_type=$1
branch_name=$2

case "$deployment_type" in
"feature")
    echo "Renaming build files for feature branch..."
    rm build/libs/LGP-1.1.jar
    mv build/libs/LGP-core-1.1.jar build/libs/LGP-core-"$branch_name".jar
    mv build/libs/LGP-examples-1.1.jar build/libs/LGP-examples-"$branch_name".jar
    ls build/libs
    ;;
"release")
    echo "Renaming build files for release branch..."
    rm build/libs/LGP-1.1.jar
    mv build/libs/LGP-core-1.1.jar build/libs/LGP-core-1.2-beta.jar
    mv build/libs/LGP-examples-1.1.jar build/libs/LGP-examples-1.2-beta.jar
    ls build/libs
    ;;
*)
    echo "Invalid deployment type"
    ;;
esac
