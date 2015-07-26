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
(ns freecoin.integration.storage-helpers
  (:require [monger.collection :as mc]
            [freecoin.storage :as storage]))

(def test-db-config {:host "127.0.0.1"
                     :port 27017
                     :db-name "freecoin_test_db"
                     :url "mongodb://127.0.0.1:27017/freecoin_test_db"})

(defn drop-collection [db-connection collection]
  (mc/drop (:db db-connection) collection))

(defn clear-db [db-connection]
  (do (drop-collection db-connection "wallets")
      (drop-collection db-connection "confirms")
      (drop-collection db-connection "secrets")))
