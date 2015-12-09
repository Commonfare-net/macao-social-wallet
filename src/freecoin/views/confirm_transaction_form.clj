(ns freecoin.views.confirm-transaction-form
  (:require [freecoin.form_helpers :as fh]
            [clavatar.core :as clavatar]
            [freecoin.routes :as routes]
            [freecoin.config :as config]))

(defn confirm-transaction-form-spec [confirmation-uid show-pin-entry]
  (let [submit {:name :submit
                :type :submit
                :class "func--confirm-transaction-form--submit"}
        ]
    {:renderer :bootstrap3-stacked
     :fields (if show-pin-entry
               [{:name :secret
                 :label "Enter your secret PIN to confirm"}
                submit]
               [submit])
     :action (routes/absolute-path (config/create-config)
                                   :post-confirm-transaction-form
                                   :confirmation-uid confirmation-uid)
     :validations (if show-pin-entry
                    [ [:required [:secret] "Please enter your PIN"]]
                    [])
     :method "post"}))

(defn render-recipient [recipient amount]
  [:ul.unstyled
   [:li {:style "margin: 1em"}
    [:span {:class "gravatar"}
     [:img {:src (clavatar/gravatar (:email recipient) :size 87 :default :mm)}]]
    [:br]
    [:span amount " -> " (:name recipient)]]])

(defn build [context show-pin-entry]
  (if-let [confirmation-uid (-> context :confirmation :uid)]
    (let [amount (-> context :confirmation :data :amount)
          recipient (-> context :recipient)]
      {:title "Confirm transaction"
       :heading "Please confirm to execute transaction"
       :body [:div {}
              [:div {:class "transaction-recipient-confirm"}
               (render-recipient recipient amount)]
              (fh/render-form (confirm-transaction-form-spec confirmation-uid show-pin-entry) (:request context))]
       })))
