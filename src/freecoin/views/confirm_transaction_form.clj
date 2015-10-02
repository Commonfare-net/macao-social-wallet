(ns freecoin.views.confirm-transaction-form
  (:require [formidable.core :as fc]
            [freecoin.views :as fv]
            [freecoin.routes :as routes]
            [freecoin.config :as config]))

(defn confirm-transaction-form-spec [confirmation-uid]
  {:fields [{:name :submit :type :submit :class "func--confirm-transaction-form--submit"}]
   :action (routes/absolute-path (config/create-config)
                                 :post-confirm-transaction-form
                                 :confirmation-uid confirmation-uid)
   :method "post"})

(defn build [context]
  (let [confirmation-uid (:confirmation-uid context)]
    {:title "Confirm transaction"
     :heading "Please confirm to execute transaction"
     :body (fc/render-form (confirm-transaction-form-spec confirmation-uid))}))
