(ns freecoin.views.reset-password
  (:require [freecoin.form_helpers :as fh]
            [freecoin.translation :as t]))

(def reset-password-form
  {:renderer :bootstrap3-stacked
   :fields [{:name :new-password :type :password :class "func--reset-pswrd-new"}
            {:name :repeat-password :type :password :class "func--reset-pswrd-repeat"}]
   :validations [[:required [:new-password :repeat-password]]
                 [:matches #"^[^\n ]{8,}$" :new-password "The password should be Minimum 8 characters"]
                 [:equal [:new-password :repeat-password] "The conformation password has to be the same as the password"]]
   :action (routes/path :reset-password)
   :method "post"})

(defn build [context]
  {:title (t/locale [:reset-password :title])
   :heading (t/locale [:reset-password :heading])
   :body-class "func--reset-password--body"
   :body [:div 
          [:h1 (t/locale [:reset-password :heading])]
          [:div
           [:div (fh/render-form reset-password-form (:request context))]]]})
