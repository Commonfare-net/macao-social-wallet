;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

;; Sourcecode designed, written and maintained by
;; Aspasia Beneti <aspra@dyne.org>

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

(ns freecoin.email-activation
  (:require [postal.core :as postal]
            [freecoin.routes :as routes]
            [freecoin.db.account :as account]
            [freecoin.db.mongo :as mongo]
            [taoensso.timbre :as log]))

(defn- generate-activation-id []
  (fxc.core/generate 32))

(defn- activation-link [activationid email]
  (routes/absolute-path :activate-account 
                        :email email
                        :activation-id activationid))

(defprotocol Email
  "Construct the activation URL, send the email and register the activation-id to the DB"
  (email-activation! [this email]))

(defrecord ActivationEmail [conf account-store]
  Email
  (email-activation! [_ email]
    (let [activation-id (generate-activation-id)
          email-response       (postal/send-message 
                                {:host (:freecoin-email-server conf)
                                 :user (:freecoin-email-user conf)
                                 :pass (:freecoin-email-pass conf)
                                 :ssl true}
                                {:from (:freecoin-email-address conf)
                                 :to [email]
                                 :subject "Please activate your freecoin account"
                                 :body (str "Please click to activate your account " (activation-link activation-id email))})]
      (account/update-activation-id! account-store email activation-id)
      (if (= :SUCCESS (:error email-response))
        email-response
        false))))

(defrecord StubActivationEmail [emails account-store]
  Email
  (email-activation! [_ email]
    (let [activation-id (generate-activation-id)]
      ;; the SUCCESS is needed to imitate poster responses
      (swap! emails conj {:email email :activation-url (activation-link activation-id email) :error :SUCCESS})
      (account/update-activation-id! account-store email activation-id) 
      (first @emails))))
