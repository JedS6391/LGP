# Documentation

## Overview

The documentation for this project is split into two sections - *API* and *Guide*.

### API

The *API* section is generated as part of the *Release* workflow, using `dokka` to build JavaDoc and KDoc content. This content is deployed to GitHub pages and made available at [https://lgp.jedsimson.co.nz/](https://lgp.jedsimson.co.nz/).

The `docs` branch is used for GitHub pages deployment. The *Release* workflow will automatically create a commit with updated API documentation on this branch.

### Guide

The *Guide* section is comprised of the source files in the `docs/source` directory. This content is deployed to [Read the Docs](https://readthedocs.org/) on pushes to the `master` branch. This content is made available at [https://docs.jedsimson.co.nz](https://docs.jedsimson.co.nz).
