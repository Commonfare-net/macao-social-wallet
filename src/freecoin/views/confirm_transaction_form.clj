(ns freecoin.views.confirm-transaction-form
  (:require [formidable.core :as fc]
            [freecoin.views :as fv]
            [clavatar.core :as clavatar]
            [freecoin.routes :as routes]
            [freecoin.config :as config]))

(defn confirm-transaction-form-spec [confirmation-uid]
  {:renderer :bootstrap3-stacked
   :fields [{:name :submit :type :submit :class "func--confirm-transaction-form--submit"}]
   :action (routes/absolute-path (config/create-config)
                                 :post-confirm-transaction-form
                                 :confirmation-uid confirmation-uid)
   :method "post"})

(defn build [context]
  (if-let [confirmation-uid (-> context :confirmation :uid)]
    (let [amount (-> context :confirmation :data :amount)
          recipient-name (-> context :recipient :name)
          recipient-email (-> context :recipient :email)]
      {:title "Confirm transaction"
       :heading "Please confirm to execute transaction"
       :body [:div {}
              [:div {:class "transaction-recipient-confirm"}
               [:ul {:style "list-style-type: none;"}
                     [:li {:style "margin: 1em"}
                      [:span {:class "gravatar"}
                       [:img {:src (clavatar/gravatar recipient-email :size 87 :default :mm)}]]
                      [:br]
                      [:span amount " -> " recipient-name]]]]
               (fc/render-form (confirm-transaction-form-spec confirmation-uid))]
       })))
