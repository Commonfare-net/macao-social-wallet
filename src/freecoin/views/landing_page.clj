(ns freecoin.views.landing-page)

(defn landing-page [context]
  (let [sign-in-url (:sign-in-url context)]
    {:body-class "func--landing-page"
     :body [:h2 [:a {:class "clj--sign-in-link"
                :href sign-in-url}
            "Sign in"]]
     :title "Welcome to Freecoin"}))
