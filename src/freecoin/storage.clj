(ns freecoin.storage
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.conversion :refer [from-db-object]]
            [monger.operators :refer :all]

            [freecoin.utils :as utils]
            ))

(defn connect [{:keys [url] :as db-config}]
  (mg/connect-via-uri url))

(defn disconnect [{:keys [conn] :as db-connection}]
  (mg/disconnect conn))

(defn insert [connection coll doc]
  (mc/insert-and-return (:db connection) coll doc))

(defn find-by-id [connection coll id]
  (if (nil? id) {:error "id is null"}
      (mc/find-map-by-id (:db connection) coll id)))

(defn find-by-key [connection coll needle]
  (mc/find-maps (:db connection) coll needle))

(defn find-all [connection coll]
  (mc/find-maps (:db connection) coll))

(defn find-one [connection coll needle]
  (mc/find-one-as-map (:db connection) coll needle))

(defn remove-by-id [connection coll id]
  (if (nil? id) {:error "id is null"}
      (mc/remove-by-id (:db connection) coll id)))

(defn aggregate [connection coll formula]
  (mc/aggregate (:db connection) coll formula))
