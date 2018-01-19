# MACAO Social Wallet - webapp crypto wallet built with the Freecoin toolkit

[![software by Dyne.org](https://www.dyne.org/wp-content/uploads/2015/12/software_by_dyne.png)](http://www.dyne.org)

Freecoin aims to be a framework for remuneration and authentication supporting multi-sig and off-line transactions on top of multiple blockchain backends. It is open source, written in Clojure and comprising of a REST API and a clean user interface. Freecoin's main use-case is that of developing "social wallets" where balances and transactions are trasparent to entire groups of people to help participatory budgeting activities and organisational awareness.

[![Build Status](https://travis-ci.org/Commonfare-net/macao-social-wallet.svg?branch=master)](https://travis-ci.org/Commonfare-net/macao-social-wallet)

[![Code Climate](https://codeclimate.com/github/Commonfare-net/macao-social-wallet.png)](https://codeclimate.com/github/Commonfare-net/macao-social-wallet)

## Design
https://freecoin.dyne.org/images/freecoin_logo.png
[![Freecoin Cornucopia](
The design of Freecoin is informed by an extensive economic and user-centered research conducted by the D-CENT project and documented in deliverables that are available to the public:

- [Design of Social Digital Currency (D4.4)](http://dcentproject.eu/wp-content/uploads/2015/10/design_of_social_digital_currency_publication.pdf)
- [Implementation of digital social currency infrastructure (D5.5)](http://dcentproject.eu/wp-content/uploads/2015/10/D5.5-Implementation-of-digital-social-currency-infrastructure-.pdf).

More resources can be found on the D-CENT webpage: http://dcentproject.eu/resource_category/publications/

Furthermore, Freecoin's first social wallet pilots are informed by the research made in the [Commonfare project](http://pieproject.eu).

## Configuration

- The conf can be found in project.clj
- Add an email-conf.edn file and point to it through the conf in project.clj under profiles. It is needed for the system ti be able to send emails when needed (eg. when a user signs up). The file should be of the form:
`{:email-server "" 
  :email-user "" 
  :email-pass "" 
  :email-address ""}`

## Running the app inside a Vagrant virtual machine

Install the **latest** version of Vagrant and VirtualboxISO (be warned, most distributions have outdated packages which won't function well)

Then go into the `ops/` directory in Freecoin and run `vagrant up`, this will create and provision a new virtual machine running Freecoin.

## Running the app locally

Install all necessary dependencies, for instance using the following packages found on APT based systems:

```
openjdk-7-jdk mongodb libversioneer-clojure haveged mongodb-server
```

then install Leiningen which will take care of all Clojure dependencies

```
mkdir ~/bin
wget https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein -O ~/bin/lein
chmod +x ~/bin/lein
```

then start the MongoDB server in which Freecoin will store its data:

```
sudo service mongod start
```

then from inside the Freecoin source, start it with

```
lein ring server
```

This command will open a browser on localhost port 8000

### Running the app using the uberjar

- For freecoin
 java -cp target/uberjar/macao-social-wallet-<VERSION>-standalone.jar freecoin.main

## Running the app from a live repl (for developers)

The server can be started and stopped from the repl by doing the following

```
$ lein repl
macao-social-wallet.core=> (start) ;; starts the server
macao-social-wallet.core=> (stop) ;; stops the server
macao-social-wallet.core=> (use macao-social-wallet.handlers.debug :reload) (stop) (start) ;; refresh specific namespaces
```

## Live reloading of .clj modules in the repl

Every time you change a file, the tracker will reload it in the
running VM and show a message in the corner of your screen (using
`notify-send`; Linux only for now):

```
lein repl
user=> (use 'macao-social-wallet.dev)
user=> (start-nstracker) ;; starts the file change tracker
```


## Running the tests

Freecoin comes complete with test units which are run by the CI but can also be run locally.

### Run all tests

For the purpose we use Clojure's `midje` package, to be run with:

```
lein midje
```

See: https://github.com/marick/Midje/wiki/A-tutorial-introduction for advanced testing features.

Bare in mind that it can be time consuming as some tests are waiting in order to test the DB expiration. On travis all tests will be run by default but we recommend that you run only the fast tests during development like bellow

### Run only the fast tests

Some of the tests are marked as slow. If you want to avoid running them you cn either

`lein midje :filter -slow`

or use the alias

`lein test-basic`

### Autotest

Autotesting can be enabled, which will run all relevant tests when source code changes. To enable that add `autotest` at the end of the lein test command. Works for the basic testing alias as well like

`lein test-basic :autotest`

## License


This Free and Open Source research and development activity is funded by the European Commission in the context of Collective Awareness Platforms for Sustainability and Social Innovation (CAPSSI) grants nr.610349 and nr.687922.

The Freecoin toolkit is Copyright (C) 2015-2018 by the Dyne.org Foundation, Amsterdam

Freecoin development is lead by Aspasia Beneti <aspra@dyne.org>

Freecoin co-design is lead by Denis Roio <jaromil@dyne.org> and Marco Sachy <radium@dyne.org>

With expert contributions by Carlo Sciolla, Duncan Mortimer, Arjan Scherpenisse, Amy Welch, Gareth Rogers, Joonas Pekkanen, Thomas König and Enric Duran.

The Freecoin "cornucopia" logo is an artwork by Andrea Di Cesare.


```
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
```
