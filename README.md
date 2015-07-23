# Freecoin - digital social currency toolkit

[![Build Status](https://travis-ci.org/d-cent/freecoin.svg?branch=master)](https://travis-ci.org/d-cent/freecoin)

Freecoin aims to be a framework for remuneration and authentication supporting multi-sig and off-line transactions on top of multiple blockchain backends. It is open source, written in Clojure and comprising of a REST API and a clean user interface.

API design is work in progress on http://freecoin.ch/api

## Configuration

Application configuration is done using `environ.clj` (see https://github.com/weavejester/environ).

For development, provide configuration variables in the `profiles.clj` file in the freecoin root directory.
E.G.: To provide values for twitter OAuth tokens:

```
{:dev-local {:env {:twitter-consumer-token "YOUR_CONSUMER_TOKEN_FROM_TWITTER"
                   :twitter-secret-token "YOUR_SECRET_TOKEN_FROM_TWITTER"}}}
```

For deployment, configuration can be provided via environment variables:
```
TWITTER_CONSUMER_TOKEN="YOUR_CONSUMER_TOKEN_FROM_TWITTER"\
TWITTER_SECRET_TOKEN="YOUR_SECRET_TOKEN_FROM_TWITTER"\
java -jar freecoin.jar
```

## Running the app from the repl

The server can be started and stopped from the repl by doing the following

```
$ lein repl
user=> (use 'freecoin.core)
user=> (start) ;; starts the server
user=> (stop) ;; stops the server
```

## Running the app using ring server

```
$ lein ring server
```

Will open a browser on localhost port 8000

## Running the tests

```
$ lein midje
```
or
```
$ ./go.sh ;; convenience script for running all tests
```
See: https://github.com/marick/Midje/wiki/A-tutorial-introduction for advanced testing features.

## Deployment

Deployment to a digital-ocean-like VM:

1) Build using ```lein uberjar```

2) Deploy to the dob_vm with:

   ```
   $ cd ops/
   $ vagrant up dob_vm
   $ ./deploy_vagrant.sh
   ```

The site should then be accessible at http://192.168.50.81:5000

## Usage

WIP

## License

Part of Decentralized Citizen Engagement Technologies (D-CENT)

R&D funded by the European Commission (FP7/CAPS 610349) 

```
Copyright (C) 2015 Dyne.org foundation
Copyright (C) 2015 Thoughtworks, Inc.
```

```
Designed and maintained by Denis Roio <jaromil@dyne.org>
With contributions by:
Gareth Rogers <grogers@thoughtworks.com>
Duncan Mortimer <dmortime@thoughtworks.com>
Andrei Biasprozvanny <abiaspro@thoughtworks.com>
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

