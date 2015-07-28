#!/bin/bash

mvn package
sudo docker build -t ehri-search-tools .
