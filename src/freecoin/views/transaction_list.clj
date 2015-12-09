(ns freecoin.views.transaction-list
  (:require [freecoin.routes :as routes]
            [clojure.tools.logging :as log]
            [freecoin.config :as config]))

(defn build [list]
  {:title "Transaction list"
   :heading "Transaction list"
   :body-class "func--transactions-page--body"
   :body
   [:table.func--transactions-page--table
    [:thead
     [:tr
      [:th "From"]
      [:th "To"]
      [:th "Amount"]
      [:th "Time"]]]
    [:tbody
     (map (fn [t]
            [:tr
             [:td (:from-id t)]
             [:td (:to-id t)]
             [:td (:amount t)]
             [:td (:timestamp t)]])
          list)
     ]
    ]
   })
