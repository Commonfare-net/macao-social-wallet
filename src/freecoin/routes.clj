;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>
;; Gareth Rogers <grogers@thoughtworks.com>
;; Duncan Mortimer <dmortime@thoughtworks.com>
;; Andrei Biasprozvanny <abiaspro@thoughtworks.com>

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

(ns freecoin.routes
  (:require [clojure.tools.logging :as log]
            [scenic.routes :as scenic]
            [bidi.bidi :as bidi]
            [freecoin.config :as config]))

(def routes (scenic/load-routes-from-file "routes.txt"))

(defn path [action & params]
  (try
    (apply bidi/path-for routes action params)
    (catch Exception e
      (log/warn (format "Key: '%s' probably does not match a route.\n%s" action e))
      (throw (Exception. (format "Error constructing url for action '%s', with params '%s'" action params))))))

(defn absolute-path [config-m action & params]
  (str (config/base-url config-m) (apply path action params)))
