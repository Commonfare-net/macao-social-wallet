(ns freecoin.views.email-confirmation
  (:require [freecoin.translation :as t]
            [taoensso.timbre :as log]))

(defn build [context]
  {:title (t/locale [:email-confirmation :title])
   :heading (t/locale [:email-confirmation :heading])
   :body-class "func--email-confirmation--body"
   :body [:div {}
          [:div (t/locale [:email-confirmation :text])
           (t/locale [:email-confirmation :text2])]]})
