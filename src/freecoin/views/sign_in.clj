(ns freecoin.views.sign-in
  (:require [freecoin.form_helpers :as fh]
            [freecoin.routes :as routes]
            [freecoin.config :as config]
            [freecoin.translation :as t]
            [taoensso.timbre :as log]))

(def sign-up-form
  {:renderer :bootstrap3-stacked
   :fields [{:name :first-name :type :text}
            {:name :last-name :type :text}
            {:name :email :type :email}
            {:name :password :type :password}]
   :validations [[:required [:first-name :last-name :email :password]]
                 [:min-length 8 :password]]
   :action (routes/path :create-account)
   :method "post"})

(def sign-in-form
  {:renderer :bootstrap3-stacked
   :fields [{:name :email :type :email} 
            {:name :password :type :password}]
   :validations [[:required [:email :password]]]
   :action (routes/path :log-in)
   :method "post"})

(defn build [context]
  {:title (t/locale [:sign-in :title])
   :heading (t/locale [:sign-in :heading])
   :body-class "func--login-page--body"
   :body [:div {}
          [:div (t/locale [:sing-in :heading])]
          [:div {:class "func-sign-in"}
           (fh/render-form sign-in-form (:request context))]
          [:div {:class "func-sign-up"}
           (fh/render-form sign-up-form (:request context))]]})
