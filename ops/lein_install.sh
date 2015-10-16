#!/bin/bash
# script to install lein from github
#

# move to the right spot
  cd /home/freecoin

# make sure we have a ./bin directory 
  [ -d ./bin ] || mkdir ./bin

# get lein and make it install itself
  wget -q -O ./bin/lein https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
  chmod 755 ./bin/lein
  ./bin/lein
 