(ns freecoin.views.transaction-form
  (:require [formidable.core :as fc]
            [freecoin.config :as config]
            [freecoin.routes :as routes]))

(def transaction-form-spec
  {:renderer :bootstrap3-stacked
   :fields [{:name :amount :type :decimal :class "func--transaction-form--amount"}
            {:name :recipient :type :text :class "func--transaction-form--recipient"}
            {:name :submit :type :submit :class "func--transaction-form--submit"}]
   :validations [[:required [:amount :recipient] :required]
                 [:min-val 0.01 [:amount] :too-small]
                 [:decimal [:amount] :type-mismatch]
                 [:string [:recipient] :type-mismatch]]
   :validate-types false
   :action (routes/absolute-path (config/create-config) :post-transaction-form)
   :method "post"})

(defn build [_context]
  {:title "Make a transaction"
   :heading "Send freecoins"
   :body (fc/render-form transaction-form-spec)})
