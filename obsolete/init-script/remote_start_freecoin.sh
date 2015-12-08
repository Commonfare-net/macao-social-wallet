#!/bin/bash
VERSION=0.2.0-SNAPSHOT

# Halt running instance
[ -f /etc/init.d/freecoind ] && sudo service freecoind stop || true

# Set up static resources
mkdir -p /var/www/freecoin
cp -r public /var/www/freecoin

# Launch application
mkdir -p /usr/local/freecoin
cp freecoin-$VERSION-standalone.jar /usr/local/freecoin
sudo cp init-script/freecoind /etc/init.d
sudo service freecoind start
