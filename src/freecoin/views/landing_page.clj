(ns freecoin.views.landing-page)

(defn landing-page [context]
  (let [sso-sign-in-link-url (:sso-sign-in-link-url context)]
    {:body [:a {:class "clj--sign-in-link"
                :href sso-sign-in-link-url}]
     :title "Welcome to freecoin"}))
