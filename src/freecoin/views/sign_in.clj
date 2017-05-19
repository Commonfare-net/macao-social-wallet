(ns freecoin.views.sign-in
  (:require [freecoin.form_helpers :as fh]
            [freecoin.routes :as routes]
            [freecoin.config :as config]
            [freecoin.translation :as t]
            [taoensso.timbre :as log]))

(def sign-up-form
  {:renderer :bootstrap3-stacked
   :fields [{:name :first-name :type :text :class "func--sign-up-first"}
            {:name :last-name :type :text :class "func--sign-up-last"}
            {:name :email :type :email :class "func--sign-up-email"}
            {:name :password :type :password :class "func--sign-up-pswrd"}
            {:name :confirm-password :type :password :class "func--sign-up-conf-pswrd"}
            {:name :submit :type :submit :class "func--sign-up-submit"}]
   :validations [[:required [:first-name :last-name :email :password :confirm-password]]
                 [:matches #"^[^\n ]{8,}$" :password "The password should be Minimum 8 characters"]
                 [:equal [:password :confirm-password] "The conformation password has to be the same as the password"]]
   :action (routes/path :sign-up-form)
   :method "post"})

(def sign-in-form
  {:renderer :bootstrap3-stacked
   :fields [{:name :sign-in-email :type :email :class "func--sign-in-email"} 
            {:name :sign-in-password :type :password :class "func--sign-in-pswrd"}
            {:name :submit :type :submit :class "func--sign-in-submit"}]
   :validations [[:required [:sign-in-email :sign-in-password]]]
   :action (routes/path :sign-in-form)
   :method "post"})

(defn build [context]
  {:title (t/locale [:sign-in :title])
   :heading (t/locale [:sign-in :heading])
   :body-class "func--login-page--body"
   :body [:div 
          [:div (t/locale [:sing-in :heading])]
          [:div {:class "col-xs-6"} 
           [:div
            [:div {:class "panel-heading"} "Sign in with an existing account"]
            [:div (fh/render-form sign-in-form (:request context))]]]
          [:div {:class "col-xs-6"}
           [:div
            [:div {:class "panel-heading"} "Create a new account!"]
            [:div (fh/render-form sign-up-form (:request context))]]]]})
