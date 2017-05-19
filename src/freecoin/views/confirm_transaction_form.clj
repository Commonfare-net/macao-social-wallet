(ns freecoin.views.confirm-transaction-form
  (:require [freecoin.form_helpers :as fh]
            [clavatar.core :as clavatar]
            [freecoin.routes :as routes]
            [freecoin.config :as config]
            [freecoin.translation :as t]))

(defn confirm-transaction-form-spec [confirmation-uid show-pin-entry]
  (let [submit {:name :submit
                :type :submit
                :class "func--confirm-transaction-form--submit"}]
    {:renderer :bootstrap3-stacked
     :fields (if show-pin-entry
               [{:name :secret
                 :label (t/locale [:transaction :enter-pin])
                 :type :password
                 :class "func--confirm-transaction-form--secret"}
                submit]
               [submit])
     :action (routes/absolute-path :post-confirm-transaction-form
                                   :confirmation-uid confirmation-uid)
     :validations (if show-pin-entry
                    [ [:required [:secret] (t/locale [:transaction :enter-pin])]]
                    [])
     :method "post"}))

(defn render-recipient [recipient amount]
  [:ul.unstyled
   [:li {:style "margin: 1em"}
    [:span {:class "gravatar"}
     [:img {:src (clavatar/gravatar (:email recipient) :size 87 :default :mm)}]]
    [:br]
    [:span amount " -> " (:email recipient)]]])

(defn build [context show-pin-entry]
  (if-let [confirmation-uid (-> context :confirmation :uid)]
    (let [amount (-> context :confirmation :data :amount)
          recipient (-> context :recipient)]
      {:title (t/locale [:transaction :confirm-title])
       :heading (t/locale [:transaction :confirm-heading])
       :body-class "func--confirmation-page--body"
       :body [:div {}
              [:div {:class "transaction-recipient-confirm"}
               (render-recipient recipient amount)]
              (fh/render-form (confirm-transaction-form-spec confirmation-uid show-pin-entry) (:request context))]})))
