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

   ))

(defn simple-form-template [{:keys [heading form-spec] :as content}]
  [:div
   [:h1 heading]
   (fc/render-form form-spec)])

(defn render-page [template {:keys [title] :as content}]
  (page/html5
    [:head
     [:title title]
     (page/include-css "//cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/2.3.2/css/bootstrap.min.css")
     (page/include-css "/css/freecoin.css")

     ]
    [:body (template content)]))

(defn parse-hybrid-form [request form-spec content-type]
  (case content-type
    "application/x-www-form-urlencoded"
    (fp/with-fallback
      (fn [problems] {:status :error :problems problems})
      {:status :ok
       :data (fp/parse-params form-spec (:params request))})

    "application/json"
    (let [data (cheshire/parse-string (autoclave/json-sanitize (slurp (:body request))) true)]
      (fp/with-fallback
        (fn [problems] {:status :error :problems problems})
        {:status :ok :data (fp/parse-params form-spec data)}))

    {:status :error :problems [{:keys [] :msg (str "unknown content type: " content-type)}]}))
