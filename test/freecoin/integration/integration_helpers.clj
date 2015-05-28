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
(ns freecoin.integration.integration-helpers
  (:require [midje.sweet :as midje]
            [clojure.data.json :as cl-json]
            [peridot.core :as p]
            [freecoin.integration.storage-helpers :as sh]
            [freecoin.core :as core]))

;; For whole-stack integration tests
(defonce app-state {})

(def test-app-params
  {:db-config sh/test-db-config
   :cookie-config {}})

(defn start-test-session [app-state]
  (assoc app-state :session (p/session (core/handler app-state))))

(defn initialise-test-session [app-state params]
  (alter-var-root #'app-state
                  #(-> %
                       (core/init params)
                       core/connect-db
                       start-test-session)))

(defn destroy-test-session [app-state]
  (when (:db-connection app-state)
    (sh/clear-db (:db-connection app-state)))
  (alter-var-root #'app-state
                  #(-> %
                       (dissoc :session)
                       core/disconnect-db)))

;; Midje checkers

(defn parse-json-string-with-entity-value-as-keyword [json-string]
  (cl-json/read-str json-string 
                    :key-fn keyword 
                    :value-fn (fn [k v] ((if (= k :entity) keyword identity) v))))

(defn json-contains [expected & options]
  (midje/chatty-checker [actual]
                         ((apply midje/contains expected options) 
                          (parse-json-string-with-entity-value-as-keyword actual))))

