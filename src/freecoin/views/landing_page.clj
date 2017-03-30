(ns freecoin.views.landing-page
  (:require [freecoin.translation :as t]
            [taoensso.timbre :as log]))

(defn landing-page [context]
  (log/info "view landing-page")
  (let [sign-in-url (:sign-in-url context)]
    {:body-class "func--landing-page"
     :body [:h2 [:a {:class "clj--sign-in-link"
                     :href (log/spy sign-in-url)}
            "Sign in"]]
     :title (t/locale [:wallet :welcome])}))
