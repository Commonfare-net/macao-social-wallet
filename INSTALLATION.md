# Freecoin installation

## Java

Java needs to be installed on the machine like
`apt-get install openjdk-7-jdk`

## MongoDB

- [Install MongoDB](https://docs.mongodb.com/manual/tutorial/install-mongodb-on-debian/)
- Start mongoDB as a daemon like `mongod --fork --logpath mongodb.log`

## Freecoin

Then create a freecoin dir and copy there the jar and the .lein-env of the uberjar build. Instead of the .lein-env file environmental variables or Java system properties can be used.

We can then start the freecoin app by running `java -cp target/uberjar/freecoin-0.2.0-standalone.jar freecoin.main`.

Freecoin is running on port 8000

Then copy to the same directory the ws sources.

The freecoin-admin tool is running on port 8990

## Troubleshooting

- No X11 DISPLAY variable was set
-- start with ring server-headless

## TODO

- Better configuration management (instead of lein-env files)
- Get rid of the ws/index manual dependency (javascript)