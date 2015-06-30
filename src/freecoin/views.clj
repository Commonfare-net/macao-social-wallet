;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>

;; With contributions by
;; Gareth Rogers <grogers@thoughtworks.com>
;; Duncan Mortimer <dmortime@thoughtworks.com>

;; This program is free software: you can redistribute it and/or modify
;; it under the terms of the GNU Affero General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.

;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU Affero General Public License for more details.

;; You should have received a copy of the GNU Affero General Public License
;; along with this program.  If not, see <http://www.gnu.org/licenses/>.

(ns freecoin.views
  (:require
   [hiccup.page :as page]
   [formative.core :as fc]
   [formative.parse :as fp]
   [cheshire.core :as cheshire]
   [autoclave.core :as autoclave]

   [json-html.core :as present]

   ))

(def response-representation
  {"application/json" "application/json"
   "application/x-www-form-urlencoded" "text/html"})


(defn confirm-page [confirmation]
  (page/html5
    [:head [:meta {:charset "utf-8"}]
     [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1"}]
     [:title (str "Confirm " (:action confirmation))]
     (page/include-css "/css/bootstrap.min.css")
     (page/include-css "/css/bootstrap-responsive.min.css")
     (page/include-css "/css/freecoin.css")
     (page/include-css "/css/json-html.css")
     ]
    [:body [:div {:class "container-fluid"}
            [:h1 (str "Confirm " (:action confirmation))]
            [:div {:class "form-shell form-horizontal bootstrap-form"}
             (present/edn->html (:data confirmation))
             [:form {:action "/confirmations" :method "post"}
              [:input {:name "id" :type "hidden"
                       :value (:_id confirmation)}]
              [:fieldset {:class "fieldset-submit"}
               [:div {:class "form-actions submit-group control-group submit-row" :id "row-field-submit"}
                [:div {:class "empty-shell"}]
                [:div {:class="input-shell"}
                 [:input {:class "btn btn-primary" :id "field-submit"
                          :name "submit" :type "submit"
                          :value "Confirm"}]]]]]]]])
  )

(defn render-page [template {:keys [title] :as content}]
  (page/html5
    [:head [:meta {:charset "utf-8"}]
     [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1"}]
     [:title title]
     (page/include-css "/css/bootstrap.min.css")
     (page/include-css "/css/bootstrap-responsive.min.css")
     (page/include-css "/css/freecoin.css")
     ]
    [:body [:div {:class "container-fluid"}
            (template content)]]))

(defn parse-hybrid-form [request form-spec content-type]
  (case content-type
    "application/x-www-form-urlencoded"
    (fp/with-fallback
      (fn [problems] {:status :error
                      :problems problems})
      {:status :ok
       :data (fp/parse-params form-spec (:params request))})

    "application/json"
    (let [data (cheshire/parse-string (autoclave/json-sanitize (slurp (:body request))) true)]
      (fp/with-fallback
        (fn [problems] {:status :error
                        :problems problems})
        {:status :ok
         :data (fp/parse-params form-spec data)}))

    {:status :error
     :problems [{:keys []
                 :msg (str "unknown content type: " content-type)}]}
    ))
