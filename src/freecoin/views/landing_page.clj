(ns freecoin.views.landing-page
  (:require [freecoin.translation :as t]
            [taoensso.timbre :as log]))

(defn render-error [error-message]
  [:div {:class "freecoin-error"}
   error-message])

(defn landing-page [context]
  (let [sign-in-url (:sign-in-url context)]
    {:body-class "func--landing-page"
     :body [:h2
            [:div
             (when-let [error-message (:error context)]
               (render-error error-message))
             [:div [:a {:class "clj--sign-in-link"
                        :href sign-in-url}
                    "Sign in"]]]]
     :title (t/locale [:wallet :welcome])}))
