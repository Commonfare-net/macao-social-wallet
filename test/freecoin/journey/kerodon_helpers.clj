(ns freecoin.journey.kerodon-helpers
  (:require [freecoin.db.mongo :as mongo]))

(defn debug
  ([state & keys]
   (clojure.pprint/pprint (select-keys state keys))
   state)
  ([state]
   (clojure.pprint/pprint state)
   state))

(defn store-contents [state stores-m]
  (prn "Wallet store contents: ")
  (clojure.pprint/pprint (mongo/query (:wallet-store stores-m) {}))
  state)
