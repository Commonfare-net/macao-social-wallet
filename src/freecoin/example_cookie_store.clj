(ns freecoin.example-cookie-store
  (:require [ring.middleware.session.store :as store]
            [clojure.tools.reader.edn :as edn]
            ))

(defn fake-encryption
  "Example of where to plug in encryption"
  [k v]
  (str "encrypted-with-" k ":" v))

(defn -example-write
  "Encrypts a data structure and returns it as a string"
  [data k]
  (fake-encryption k (pr-str data)))

(defn fake-unencryption
  "Example of where to plug in unencryption"
  [k data]
  (edn/read-string (second (clojure.string/split data #":" 2))) )

(defn -example-read
  "Unencryptes a string and returns it as a data structure"
  [data k]
  (if data
    (fake-unencryption k data)
    {}))

(defn -example-delete
  "Replaces data with an empty map"
  []
  {})

(deftype ExampleStore [not-so-secret-key]
  store/SessionStore
  (read-session [_ data] (-example-read data not-so-secret-key))
  (write-session [_ _ data] (-example-write data not-so-secret-key))
  (delete-session [_ _] (-example-delete)))

(defn new-example-store [not-so-secret-key]
  (ExampleStore. not-so-secret-key))
