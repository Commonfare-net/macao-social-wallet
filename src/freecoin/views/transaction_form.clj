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
            {:name :tags
             :label (t/locale [:transaction :tags])
             :type :tags
             :class "func--transaction-form--tags"}
            {:name :submit
             :type :submit
             :class "func--transaction-form--submit"}]
   :validations [[:required [:amount :recipient]
                  (t/locale [:transaction :required-field])]
                 [:decimal [:amount]
                  (t/locale [:transaction :invalid-amount])]
                 [:min-val 0.01 [:amount]
                  (t/locale [:transaction :too-small-amount])]
                 [:string [:recipient]
                  (t/locale [:transaction :invalid-recipient])]]
   :validate-types false
   :action (routes/absolute-path :post-transaction-form)
   :method "post"})

(defn build [request]
  {:title (t/locale [:transaction :make])
   :heading (t/locale [:transaction :send])
   :body (fh/render-form transaction-form-spec request)})
