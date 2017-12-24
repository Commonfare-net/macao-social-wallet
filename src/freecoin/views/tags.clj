;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2016 Dyne.org foundation

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>
;; Carlo Sciolla <carlo.sciolla@gmail.com>

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

(ns freecoin.views.tags
  (:require [auxiliary.translation :as t]
            [freecoin.routes :as http]))

(defn tag-link
  [name]
  [:a {:href (http/path :get-tag-details :name name)} name])

(defn build-html [tags]
  {:title (t/locale [:tags :page :title])
   :heading (t/locale [:tags :page :title])
   :body-class "func--tags-page--body"
   :body
   [:div
    [:table.func--tags-page--table.table.table-striped
     [:thead
      [:tr
       [:th (t/locale [:tags :page :table :tag])]
       [:th (t/locale [:tags :page :table :count])]
       [:th (t/locale [:tags :page :table :value])]]]
     [:tbody
      (for [{:keys [tag count amount]} tags]
        [:tr
         [:td (tag-link tag)]
         [:td count]
         [:td amount]])]]]})
