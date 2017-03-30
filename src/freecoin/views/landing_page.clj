(ns freecoin.views.landing-page
  (:require [freecoin.translation :as t]
            [taoensso.timbre :as log]))

(defn render-unauthorized []
  [:div {:class "unauthorized-error"}
   "The user name or password are incorrect"])

(defn landing-page [context]
  (let [sign-in-url (:sign-in-url context)]
    {:body-class "func--landing-page"
     :body [:h2
            [:div
             (when (:unauthorized context)
               (render-unauthorized))
             [:div [:a {:class "clj--sign-in-link"
                        :href (log/spy sign-in-url)}
                    "Sign in"]]]]
     :title (t/locale [:wallet :welcome])}))
