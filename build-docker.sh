#!/bin/bash

mvn package
sudo docker build -t ehri/ehri-search-tools .
