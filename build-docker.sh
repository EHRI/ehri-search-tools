#!/bin/bash

tag=${1:-ehri/ehri-search-tools}

mvn package
sudo docker build -t $tag .
