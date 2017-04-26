#!/usr/bin/env zsh
#
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

repo=https://github.com/pienews/freecoin

print "== entering freecoin provisioning"

# TODO: check if already created
useradd -m freecoin
print "freecoin:freecoin" | chpasswd
chsh -s /bin/zsh freecoin

# TODO: check if already installed
print "== start installing openJDK and mongodb"
# apt-get -q -y update
# apt-get --yes --force-yes upgrade
apt-get --yes --force-yes --no-install-recommends install openjdk-7-jdk mongodb
# apt-get clean
print "== end installing openJDK and mongodb"

[[ -x /usr/local/bin/lein ]] || {
	print "== installing leiningen"
	curl https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein \
		 -o  /usr/local/bin/lein
	chmod +x /usr/local/bin/lein
	print "== end installing leiningen"
}
# print y | LEIN_ROOT=1 lein upgrade 2> /dev/null


[[ -r /home/freecoin/freecoin-src ]] || {
	print "== start cloning freecoin"
	pushd /home/freecoin
	touch .zshrc
	sudo -u freecoin git clone $repo freecoin-src
	pushd freecoin-src
	sudo -u freecoin git submodule update --init
	print "== end cloning freecoin"
}

# TODO: perhaps git pull --rebase if updated

print "== start building freecoin"
sudo -u freecoin lein deps
sudo -u freecoin lein compile
print "== over building freecoin"

# start freecoin (and also at every boot from rc.local)
cat <<EOF > /etc/rc.local
HOME=/home/freecoin &&
	cd /home/freecoin/freecoin-src &&
	sudo -b -u freecoin lein ring server-headless
exit 0
EOF
HOME=/home/freecoin &&
	cd /home/freecoin/freecoin-src &&
	sudo -b -u freecoin lein ring server-headless
