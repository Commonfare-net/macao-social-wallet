(ns freecoin.views.transaction-form
  (:require [formidable.core :as fc]
            [freecoin.routes :as routes]))

(def transaction-form-spec
  {:fields [{:name :amount :type :decimal}
            {:name :recipient :type :text}]
   :validations [[:required [:amount :recipient] :required]
                 [:min-val 0.01 [:amount] :too-small]
                 [:decimal [:amount] :type-mismatch]
                 [:string [:recipient] :type-mismatch]]
   :validate-types false
   :action (routes/path :post-transaction-form)
   :method "post"})

(defn build [_content]
  {:title "Make a transaction"
   :heading "Send freecoins"
   :form-spec (fc/render-form transaction-form-spec)})
