#!/bin/bash
# script to install lein from github
#

# check for existence of ~/bin 
  [ -d ./bin ] || mkdir ./bin

# get lein and make it install itself
  wget -q -O ./bin/lein https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
  chmod 755 ./bin/lein
  sudo -u freecoin ./bin/lein
 