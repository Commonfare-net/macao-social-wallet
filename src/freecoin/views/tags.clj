(ns freecoin.views.tags)

(defn build-html [tags]
  {:title "Tags"
   :heading "Tags"
   :body-class "func--tags-page--body"
   :body
   [:div
    [:table.func--tags-page--table.table.table-striped
     [:thead
      [:tr
       [:th "Tag"]
       [:th "# tagged transactions"]]]
     [:tbody
      (for [{:keys [tag count]} tags]
        [:tr
         [:td tag]
         [:td count]])]]]})
