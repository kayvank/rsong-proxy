#!/bin/bash
##
## remove the old rspace and start node as validator in a fresh state
## usage ./startValidator.sh 
##

rm -rf ~/.rnode/rspace ~/.rnode/blockstore ~/.rnode/dagstorage ~/.rnode/tmp
rnode run -s \
      --required-sigs 0 \
      --thread-pool-size 5  \
      --validator-private-key \
      6bd8981cf922a547ca3c2d218f747d8048b7999a2f744f14e124a7082991b7e3


