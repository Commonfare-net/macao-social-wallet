(ns freecoin.views.transaction-form
  (:require [json-html.core :refer :all]
            [freecoin.config :as config]
            [freecoin.form_helpers :as fh]
            [freecoin.routes :as routes]
            [freecoin.translation :as t]))

(def transaction-form-spec
  {:renderer :bootstrap3-stacked
   :fields [{:name :amount
             :label (t/locale [:transaction :amount])
             :type :decimal
             :class "func--transaction-form--amount"}
            {:name :recipient
             :label (t/locale [:transaction :recipient])
             :type :email
             :class "func--transaction-form--recipient"}
            {:name :tags
             :label (t/locale [:transaction :tags])
             :type :tags
             :class "func--transaction-form--tags"}
            {:name :submit
             :type :submit
             :class "func--transaction-form--submit"}]
   :validations [[:required [:amount :recipient]
                  (t/locale [:transaction :required-field])]
                 [:decimal [:amount]
                  (t/locale [:transaction :invalid-amount])]
                 [:min-val 0.01 [:amount]
                  (t/locale [:transaction :too-small-amount])]
                 [:string [:recipient]
                  (t/locale [:transaction :invalid-recipient])]]
   :validate-types false
   :action (routes/absolute-path :post-transaction-form)
   :method "post"})

(defn build
  ([request] (build request transaction-form-spec))
  ([request spec]
   {:title (t/locale [:transaction :make])
    :heading (t/locale [:transaction :send])
    :body (fh/render-form spec request)}))

;; build a more complex transaction form with hidden fields
;; not using formalize here, but hiccup directly
(defn build-transaction-to [ctx]
  (if-let [email (get-in ctx [:request :params :email])]
    {:title   (str (t/locale [:transaction :make]) " -> " email)
     :heading (str (t/locale [:transaction :send]) " -> " email)
     :body [:form {:action (routes/absolute-path :post-transaction-form)
                   :class "func--transaction-to-body"
                   :method "POST"}
            [:input {:name "__anti-forgery-token"
                     :type "hidden"
                     :value (get-in ctx [:request :session "__anti-forgery-token"])}]
            [:input {:name "recipient"
                     :type "hidden"
                     :value email}]

            ;; TODO: make this fieldset rendering into a form-helper
            [:fieldset {:class "fieldset-amount"}
             [:div {:class "form-group"}
              [:label {:class "control-label"
                       :for   "field-amount"} "Amount"]
              [:input {:class "form-control func--transaction-to-amount"
                       :id    "field-amount"
                       :name "amount"
                       :type "decimal"
                       :value ""}]]
             [:div {:class "form-group"}
              [:label {:class "control-label"
                       :for   "field-tags"} "Tags"]
              [:input {:class "form-control func--transaction-to-tags"
                       :id    "field-tags"
                       :name "tags"
                       :type "decimal"
                       :value ""}]]
             ]

            [:fieldset {:class "fieldset-submit"}
             [:div {:class "form-group"}
              [:span {:class "visible-xs-inline-block visible-sm-inline-block visible-md-inline-block visible-lg-inline-block"}
               [:input {:class "form-control btn btn-primary func--transaction-to-submit"
                        :id "field-submit"
                        :name "submit"
                        :type "submit"}]]]]
            ]}

    ))
