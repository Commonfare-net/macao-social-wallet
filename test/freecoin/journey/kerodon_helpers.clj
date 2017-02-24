(ns freecoin.journey.kerodon-helpers
  (:require [clojure.string :as string]
            [net.cgrand.enlive-html :as html]
            [freecoin.db.mongo :as mongo]))

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

(defn remember [state memory-atom key state->value]
  (swap! memory-atom assoc key (state->value state))
  state)

(defn recall [memory-atom key]
  (get @memory-atom key))

(defn state-on-account-page->email [state]
  (let [uri (get-in state [:request :uri])]
    (-> state
        (get-in [:request :uri])
        (string/split #"/")
        last)))

(defn body-selector-count [selector]
  (fn [state]
    (let [matches (-> state :enlive (html/select selector))]
      (count matches))))
