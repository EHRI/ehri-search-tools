#!/bin/bash

mvn package -DskipTests
sudo docker build -t ehri/ehri-search-tools .
