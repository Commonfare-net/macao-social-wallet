;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>

;; With contributions by
;; Arjan Scherpenisse <arjan@scherpenisse.net>

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

(ns freecoin.form_helpers
  (:require
   [formidable.core :as fc]
   [formidable.parse :as fp]
   ))

(defn render-form [form-spec request]
  [:div
   (let [problems (:flash request)]
     (fc/render-form (assoc form-spec :problems problems))
     )])

(defn form-problem
  ([problems] {::form-problems problems})
  ([key msg] (form-problem [{:keys [key] :msg msg}])))

(defn flash-form-problem [response context]
  (assoc response :flash (::form-problems context)))

(defn validate-form [form-spec data]
  (fp/with-fallback
    (fn [problems] {:status :error
                    :problems problems})
    {:status :ok
     :data (fp/parse-params form-spec data)}))
