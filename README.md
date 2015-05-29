# Freecoin - digital social currency toolkit

Freecoin aims to be a framework for remuneration and authentication supporting multi-sig and off-line transactions on top of multiple blockchain backends. It is open source, written in Clojure and comprising of a REST API and a clean user interface.

API design is work in progress on http://freecoin.ch/api

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

