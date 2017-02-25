#!/usr/bin/env zsh
# Copyright (c) 2017 Dyne.org Foundation
#
# This file is part of Freecoin
#
# This source code is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This software is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this source code. If not, see <http://www.gnu.org/licenses/>.

repo=https://github.com/d-cent/freecoin

print "----- entering freecoin provisioning"

useradd -m freecoin
print "freecoin:freecoin" | chpasswd
chsh -s /bin/zsh freecoin

print "----- start installing openJDK and mongodb"
apt-get -qq -y update
apt-get --yes --force-yes upgrade
apt-get --yes --force-yes --no-install-recommends install openjdk-7-jdk mongodb
apt-get clean
print "----- end installing openJDK and mongodb"

print "----- start installing leiningen"
curl https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein \
	 -o  /usr/local/bin/lein
chmod +x /usr/local/bin/lein
print y | LEIN_ROOT=1 lein upgrade 2> /dev/null
print "----- end installing leiningen"

print "----- start cloning freecoin"
pushd /home/freecoin
touch .zshrc
sudo -u freecoin git clone $repo freecoin-src
pushd freecoin-src
sudo -u freecoin git submodule update --init
print "----- end cloning freecoin"


print "----- start building freecoin"
sudo -u freecoin lein deps
sudo -u freecoin lein compile
print "----- over building freecoin"

# TODO: verify that this command starts freecoin correctly
sudo -u freecoin lein ring server-headless
