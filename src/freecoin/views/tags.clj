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
       [:th "# tagged transactions"]
       [:th "Moved value"]]]
     [:tbody
      (for [{:keys [tag count amount]} tags]
        [:tr
         [:td tag]
         [:td count]
         [:td amount]])]]]})
