#!/bin/bash
## usage ./startValidator.sh 

rnode run -s \
      --required-sigs 0 \
      --map_size 2048576000 \
      --thread-pool-size 5
