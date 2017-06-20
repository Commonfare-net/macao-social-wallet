;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

;; Sourcecode designed, written and maintained by
;; Aspasia Beneti  <aspra@dyne.org>

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

(ns freecoin.db.password-recovery
  (:require [freecoin.db.mongo :as mongo]
            [buddy.hashers :as hashers]
            [taoensso.timbre :as log]))

(defn new-entry!
  [password-recovery-store email recovery-id]
  (mongo/store! password-recovery-store :email {:email email
                                                :created-at (java.util.Date.)
                                                :recovery-id recovery-id}))

(defn fetch-by-password-recovery-id [password-recovery-store password-recovery-id]
  (first (mongo/query password-recovery-store {:recovery-id password-recovery-id})))

(defn fetch [password-recovery-store email]
  (some-> (mongo/fetch password-recovery-store email)))

(defn remove! [password-recovery-store email]
  (mongo/delete! password-recovery-store email))


