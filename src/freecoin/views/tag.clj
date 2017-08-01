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

(ns freecoin.views.tag
  (:require [freecoin.translation :as t]
            [freecoin.form_helpers :as fh]
            [taoensso.timbre :as log]))

(defn build-html
  "Renders the tag details page content"
  [{:keys [tag count amount created created-by] :as tag-entity}]
  {:title (str (t/locale [:tag :page :title]) ": " tag)
   :heading (str (t/locale [:tag :page :title]) ": " tag)
   :body-class "func--tag-page--body"
   :body
   [:div
    [:table.func--tag-page--table.table.table-striped
     [:tbody
      [:tr.func--tag-page--count
       [:th (t/locale [:tag :page :table :count])]
       [:td count]]
      [:tr.func--tag-page--amount
       [:th (t/locale [:tag :page :table :value])]
       [:td (fh/thousant-separator amount)]]
      [:tr.func--tag-page--created-by
       [:th (t/locale [:tag :page :table :created-by])]
       [:td created-by]]
      [:tr.func--tag-page--created
       [:th (t/locale [:tag :page :table :created])]
       [:td created]]]]]})
