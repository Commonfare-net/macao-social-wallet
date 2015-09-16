(ns freecoin.views.transaction-form
  (:require [freecoin.views :as fv]
            [formidable.core :as fc]))

(def transaction-form-spec
  {:fields [{:name :amount :datatype :float}
            {:name :recipient :type :text}]
   :validations [[:required [:amount :recipient]]
                 [:min-val 0.01 [:amount]]]
   :action "/send"
   :method "post"})

(defn build [_content]
  {:title "Make a transaction"
   :heading "Send freecoins"
   :form-spec transaction-form-spec})
