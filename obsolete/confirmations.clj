(lc/defresource get-confirm-form [request code]
  :service-available?
  {::db (get-in request [:config :db-connection])
   ::content-type (get-in request [:headers "content-type"])}

  :allowed-methods [:get]
  :available-media-types ["text/html"]

  :allowed? (fn [ctx]
              (let [found (storage/find-by-id
                           (:db (::db ctx)) "confirmations" code)]

                (if (contains? found :error)
                  [false {::error (:error found)}]

                  (if (empty? found)
                    [false {::error "confirmation not found"}]
                    ;; here fill in the content of the confirmation
                    ;; {:_id     code
                    ;;  :action  name
                    ;;  :data    collection}
                    [true {::user-data found}]))
                ))

  :handle-forbidden (fn [ctx]
                      (lr/ring-response {:status 404
                                         :body (::error ctx)}))

  :handle-ok (fn [ctx] (views/confirm-button (::user-data ctx)))
  )


(defn confirm-button [{:keys [action data] :as confirmation}]
  (page/html5
    [:head [:meta {:charset "utf-8"}]
     [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1"}]
     [:title (str "Confirm " action)]
     (page/include-css "/static/css/bootstrap.min.css")
     (page/include-css "/static/css/bootstrap-responsive.min.css")
     (page/include-css "/static/css/freecoin.css")
     (page/include-css "/static/css/json-html.css")]
    [:body
     [:div {:class "container-fluid"}
      [:h1 (str "Confirm " action)]
      [:div {:class "form-shell form-horizontal bootstrap-form"}
       (present/edn->html data)
       [:form {:action "/confirmations" :method "post"}
        [:input {:name "id" :type "hidden"
                 :value (:_id confirmation)}]
        [:fieldset {:class "fieldset-submit"}
         [:div {:class "form-actions submit-group control-group submit-row" :id "row-field-submit"}
          [:div {:class "empty-shell"}]
          [:div {:class="input-shell"}
           [:input {:class "btn btn-primary" :id "field-submit"
                    :name "submit" :type "submit"
                    :value "Confirm"}]]]]]]]]))

