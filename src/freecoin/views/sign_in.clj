(ns freecoin.views.sign-in
  (:require [freecoin.form_helpers :as fh]
            [freecoin.routes :as routes]
            [freecoin.config :as config]
            [freecoin.translation :as t]
            [taoensso.timbre :as log]))

(def sign-up
  {:fields [{:name :first-name :type :text}
            {:name :last-name :type :text}
            {:name :email :type :email}
            {:name :password :type :password}]
   :validations [[:required [:first-name :last-name :email :password]]
                 [:min-length 8 :password]]
   :action (routes/path :create-account)
   :method "post"})

(def sign-in
  {:fields [{:name :user-name :type :text} 
            {:name :password :type :password}]
   :validations [[:required [:user-name :password]]]
   :action (routes/path :log-in)
   :method "get"})

(defn build [context]
  {:title (t/locale [:sign-in :title])
   :heading (t/locale [:sign-in :heading])
   :body-class "func--login-page--body"
   :body [:div {}
          [:div (t/locale [:sing-in :heading])]
          [:div {:class "func-sign-in"}
           (fh/render-form sign-in (:request context))]
          [:div {:class "func-sign-up"}
           (fh/render-form sign-up (:request context))]]})
