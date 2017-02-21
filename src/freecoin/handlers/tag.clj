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

(ns freecoin.handlers.tag
  (:require [liberator.core :as lc]
            [freecoin.views :as fv]
            [freecoin.auth :as auth]
            [freecoin.views.tag :as tv]
            [freecoin.blockchain :as blockchain]))

(lc/defresource get-tag-details [blockchain]
  :allowed-methods [:get]
  :available-media-types ["text/html"]

  :authorized? auth/is-signed-in

  :handle-ok
  (fn [ctx]
    (let [name (get-in ctx [:request :params :name])
          tag (blockchain/tag-details blockchain name {})]
      (-> (tv/build-html tag)
          fv/render-page))))
