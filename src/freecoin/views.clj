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
  (:require [hiccup.page :as page]
            [freecoin.routes :as r]
            [freecoin.translation :as t]))

(defn menu-entry
  "Turns an action key into its correspondent entry in the translation table"
  [key]
  [:top-menu key])

(defn- menu-link
  ([link]
   (menu-link link "menu-entry"))
  ([link class]
   [:li {:class class
         :role "presentation"}
    [:a {:href (r/path link)} (t/locale (menu-entry link))]]))

(defn top-menu
  "Displays the top menu with links to the various application pages"
  []
  [:div
   [:ul {:class "nav nav-pills"}
    (for [link [:landing-page
                :get-transaction-form
                :get-all-transactions
                :participants]]
      (menu-link link))
    (menu-link :sign-out "menu-entry-last")]])

(defn render-page [{:keys [title heading body body-class] :as content}]
  (page/html5
   [:head [:meta {:charset "utf-8"}]
    [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1"}]
    [:title title]
    (page/include-css "/static/css/bootstrap.min.css")
    (page/include-css "/static/css/bootstrap-theme.min.css")
    (page/include-css "/static/css/freecoin.css")
    (page/include-css "/static/css/json-html.css")]
   [:body {:class body-class}
    (top-menu)
    [:div {:class "container"}
     [:h1 (or heading title)]
     body]]))
