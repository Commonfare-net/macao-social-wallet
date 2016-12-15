(ns freecoin.views.transaction-form
  (:require [freecoin.config :as config]
            [freecoin.form_helpers :as fh]
            [freecoin.routes :as routes]
            [freecoin.translation :as t]))

(def transaction-form-spec
  {:renderer :bootstrap3-stacked
   :fields [{:name :amount
             :label (t/locale [:transaction :amount])
             :type :decimal
             :class "func--transaction-form--amount"}
            {:name :recipient
             :label (t/locale [:transaction :recipient])
             :type :text
             :class "func--transaction-form--recipient"}
            {:name :submit
             :type :submit
             :class "func--transaction-form--submit"}]
   :validations [[:required [:amount :recipient]
                  "Required field"]
                 [:min-val 0.01 [:amount]
                  "Amount too small"]
                 [:decimal [:amount]
                  "Invalid type for amount"]
                 [:string [:recipient]
                  "Invalid type for recipient"]]
   :validate-types false
   :action (routes/absolute-path :post-transaction-form)
   :method "post"})

(defn build [request]
  {:title "Make a transaction"
   :heading (str "Send freecoins")
   :body (fh/render-form transaction-form-spec request)})
