# Freecoin installation

## Java

Java needs to be installed on the machine
apt-get install openjdk-7-jdk

## MondoDB

- [Install MongoDB](https://docs.mongodb.com/manual/tutorial/install-mongodb-on-debian/)
- Start mongoDB like `service mongod start`

## Stonecutter

Copy to the same directory the jar, the test-key.json from test-resources and the .lein-env produced by lein uberjar.
Start stonecutter like `java -jar stonecutter-0.1.0-SNAPSHOT-standalone.jar`
Stonecutter is running on port 8000

## Freecoin

Because stonecutter is on SNAPSHOT version we need `export LEIN_SNAPSHOTS_IN_RELEASE=1` before we do the uberjar.
Then we can start the freecoin app by running `java -cp target/uberjar/freecoin-0.2.0-standalone.jar freecoin.main`. The jar should be on the same dir as rsources/land for the translation.
Freecoin is running on port 5000

Lastly we can start the administrative functionality of freecoin like `java -cp target/uberjar/freecoin-0.2.0-standalone.jar gorilla_repl.core`
The freecoin-admin tool is running on port 8990

## Troubleshooting

- No X11 DISPLAY variable was set
-- start with ring server-headless
