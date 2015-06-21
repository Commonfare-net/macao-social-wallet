(ns freecoin.storage
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.conversion :refer [from-db-object]]
            [monger.operators :refer :all]
            ))

(defn connect [{:keys [host port db-name] :as db-config}]
  (let [conn (mg/connect {:host host :port port})
        db (mg/get-db conn db-name)]
    {:conn conn
     :db db}))

(defn disconnect [{:keys [conn] :as db-connection}]
  (mg/disconnect conn))

(defn insert [connection coll doc]
  (mc/insert-and-return (:db connection) coll doc))

(defn find-by-id [connection coll id]
  (mc/find-map-by-id (:db connection) coll id))

(defn find-by-key [connection coll needle]
  (mc/find-maps (:db connection) coll needle))

(defn find-all [connection coll]
  (mc/find-maps (:db connection) coll))

(defn find-one [connection coll needle]
  (mc/find-one-as-map (:db connection) coll needle))

(defn remove-by-id [connection coll id]
  (mc/remove-by-id (:db connection) coll id))

(defn aggregate [connection coll formula]
  (mc/aggregate (:db connection) coll formula))
