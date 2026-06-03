#!/bin/bash

TAG=${1:-ehri/ehri-search-tools}

# Get project version from pom.xml
VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

mvn package
sudo docker build --tag $TAG:latest --tag $TAG:${VERSION} --build-arg PROJECT_VERSION=$VERSION  .
