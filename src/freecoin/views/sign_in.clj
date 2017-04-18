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
   :action (routes/path :sign-up-form)
   :method "post"})

(def sign-in-form
  {:renderer :bootstrap3-stacked
   :fields [{:name :sign-in-email :type :email} 
            {:name :sign-in-password :type :password}]
   :validations [[:required [:sign-in-email :sign-in-password]]]
   :action (routes/path :sign-in-form)
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
