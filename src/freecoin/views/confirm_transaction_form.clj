(ns freecoin.views.confirm-transaction-form
  (:require [freecoin.form_helpers :as fh]
            [clavatar.core :as clavatar]
            [freecoin.routes :as routes]
            [freecoin.config :as config]))

(defn confirm-transaction-form-spec [confirmation-uid]
  {:renderer :bootstrap3-stacked
   :fields [{:name :submit
             :type :submit
             :class "func--confirm-transaction-form--submit"}]
   :action (routes/absolute-path (config/create-config)
                                 :post-confirm-transaction-form
                                 :confirmation-uid confirmation-uid)
   :method "post"})

(defn render-recipient [recipient amount]
  [:ul.unstyled
   [:li {:style "margin: 1em"}
    [:span {:class "gravatar"}
     [:img {:src (clavatar/gravatar (:email recipient) :size 87 :default :mm)}]]
    [:br]
    [:span amount " -> " (:name recipient)]]])

(defn build [context]
  (if-let [confirmation-uid (-> context :confirmation :uid)]
    (let [amount (-> context :confirmation :data :amount)
          recipient (-> context :recipient)]
      {:title "Confirm transaction"
       :heading "Please confirm to execute transaction"
       :body [:div {}
              [:div {:class "transaction-recipient-confirm"}
               (render-recipient recipient amount)]
              (fh/render-form (confirm-transaction-form-spec confirmation-uid) (:request context))]
       })))
