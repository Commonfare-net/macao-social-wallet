(ns freecoin.views.transaction-list
  (:require [freecoin.routes :as routes]
            [freecoin.config :as config]))

(defn build [list]
  {:title "Transaction list"
   :heading "Transaction list"
   :body-class "func--transactions-page--body"
   :body
   [:table.func--transactions-page--table
    [:thead
     [:tr
      [:th "UID"]]]
    [:tbody
     (map (fn [t]
            [:tr
             [:td (:uid t)]])
          list)
     ]
    ]
   })
