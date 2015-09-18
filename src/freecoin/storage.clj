(ns freecoin.storage
  (:require [monger.core :as mg]
            [monger.collection :as mc]))

(defn connect [{:keys [url] :as db-config}]
  (mg/connect-via-uri url))

(defn disconnect [{:keys [conn] :as db-connection}]
  (mg/disconnect conn))

(defn insert [db coll doc]
  (mc/insert-and-return db coll doc))

(defn find-by-id [db coll id]
  (if (nil? id) {:error "id is null"}
      (mc/find-map-by-id db coll id)))

(defn find-by-key [db coll needle]
  (mc/find-maps db coll needle))

(defn find-all [db coll]
  (mc/find-maps db coll))

(defn find-one [db coll needle]
  (mc/find-one-as-map db coll needle))

(defn remove-by-id [db coll id]
  (if (nil? id) {:error "id is null"}
      (mc/remove-by-id db coll id)))

(defn aggregate [db coll formula]
  (mc/aggregate db coll formula))
