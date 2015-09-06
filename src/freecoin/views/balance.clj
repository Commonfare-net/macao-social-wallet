(ns freecoin.views.balance)

(defn render-wallet [wallet]
  [:li {:style "margin: 1em"}
   [:div {:class "card pull-left" }
    [:span (str "name: " (:name wallet))]
    [:br]
    [:span (str "email: " (:email wallet))]
    [:br]
    [:span {:class "qrcode pull-left"}
     [:img {:src (format "/qrcode/%s" (:name wallet))} ]]
    [:span {:class "gravatar pull-right"}
     [:img {:src (clavatar.core/gravatar (:email wallet) :size 87 :default :mm)}]]
    ]])

(defn balance-template [{:keys [wallet balance] :as content}]
   (if (empty? wallet)
     [:span (str "No wallet found")]
     [:div
      [:ul {:style "list-style-type: none;"}
      (render-wallet wallet)]
      [:div {:class "balance pull-left"}
       (str "Balance: " balance)]]
     )
   )
