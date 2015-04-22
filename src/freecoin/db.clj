(ns freecoin.db
  (:gen-class)
  (:require [monger.core :as mg]
            [monger.collection :as mc])
;            [monger.json])
  (:import ;[com.mongodb MongoOptions ServerAddress]
           [org.bson.types ObjectId]
           [com.mongodb DB WriteConcern]      
           )
  )

(def dbname "fxctest1")
(def uri (format "mongodb://127.0.0.1/%s" dbname))
(def sock {:conn :db})

(defn connect []
  ;; connect without authentication
  (def sock (mg/connect-via-uri uri))
  )

(defn disconnect []
  (mg/disconnect (:conn sock))
  )

(defn insert [doc]
  (mc/insert (:db sock) dbname doc)
  )

(defn find [doc] (mc/find-maps (:db sock) dbname doc))
(defn list [] (mc/find-maps (:db sock) dbname))

(defn find-one [doc] (mc/find-one-as-map (:db sock) dbname doc))


  ;; ;; connect with authentication
  ;; (let [uri               "mongodb://clojurewerkz/monger!:monger!@127.0.0.1/monger-test4"
  ;;       {:keys [conn db]} (mg/connect-via-uri uri)])
  
  ;; ;; connect using connection URI stored in an env variable, in this case, MONGOHQ_URL
  ;; (let [uri               (System/genenv "MONGOHQ_URL")
  ;;       {:keys [conn db]} (mg/connect-via-uri uri)])

  ;; ;; using MongoOptions allows fine-tuning connection parameters,
  ;; ;; like automatic reconnection (highly recommended for production environment)
  ;; (let [^MongoOptions opts (mg/mongo-options :threads-allowed-to-block-for-connection-multiplier 300)
  ;;       ^ServerAddress sa  (mg/server-address "127.0.0.1" 27017)
  ;;       conn               (mg/connect sa opts)]
    
   
