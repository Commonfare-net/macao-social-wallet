(ns freecoin.views.account-activated
  (:require [freecoin.translation :as t]
            [freecoin.routes :as routes]))

(defn build [context]
  {:title (t/locale [:account-activated :title])
   :heading (t/locale [:account-activated :heading])
   :body-class "func--account-activated--body"
   :body [:div {}
          [:div (t/locale [:account-activated :text])
           [:a {:href (routes/absolute-path :sign-in)} "Sign in"]]]})

