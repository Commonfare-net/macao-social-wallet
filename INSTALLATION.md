# Freecoin installation

## Java

Java needs to be installed on the machine like
`apt-get install openjdk-7-jdk`

## MongoDB

- [Install MongoDB](https://docs.mongodb.com/manual/tutorial/install-mongodb-on-debian/)
- Start mongoDB as a daemon like `mongod --fork --logpath mongodb.log`

## Stonecutter

Copy to the same directory the jar, the test-key.json from test-resources and the .lein-env produced by lein uberjar.

Start stonecutter like `java -jar stonecutter-0.1.0-SNAPSHOT-standalone.jar`

Stonecutter is running on port 5000

## Freecoin

Because stonecutter is on SNAPSHOT version we need `export LEIN_SNAPSHOTS_IN_RELEASE=1` before we do the uberjar.

Before the build a freecoin entry needs to be added to resources/client-credentials.yml for freecoin with the correct URL.

Then create a freecoin dir and copy there the jar and the .lein-env of the uberjar build. Instead of the .lein-env file environmental variables or Java system properties can be used.

We can then start the freecoin app by running `java -cp target/uberjar/freecoin-0.2.0-standalone.jar freecoin.main`.

Freecoin is running on port 8000

Then copy to the same directory the ws sources.

The freecoin-admin tool is running on port 8990

## Troubleshooting

- No X11 DISPLAY variable was set
-- start with ring server-headless

- Forbidden when redirecting to stonecutter
-- The credentials.yml is not properly set up

## TODO

- Better configuration management (instead of lein-env files)
- Get rid of the ws/index manual dependency (javascript)