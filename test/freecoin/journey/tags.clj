;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2017 Dyne.org foundation

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>
;; Aspasia Beneti <aspra@dyne.org>

;; With contributions by
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

(ns freecoin.journey.tags
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [freecoin.journey.kerodon-selectors :as ks]
            [freecoin.journey.kerodon-checkers :as kc]
            [freecoin.journey.kerodon-helpers :as kh]
            [freecoin.test-helpers.integration :as ih]
            [freecoin.journey.helpers :as jh]
            [freecoin-lib.db.freecoin :as db]
            [freecoin-lib.core :as blockchain]
            [freecoin.routes :as routes]
            [freecoin-lib.config :as c]
            [freecoin-lib.db.account :as account]
            [simple-time.core :as time]
            [taoensso.timbre :as log]))

(ih/setup-db)

(def stores-m (db/create-freecoin-stores (ih/get-test-db)))
(def blockchain (blockchain/new-mongo stores-m))

(def test-app (ih/build-app {:stores-m stores-m
                             :blockchain blockchain
                             :email-activator (freecoin.email-activation/->StubActivationEmail
                                               (atom [])
                                               (:account-store stores-m))}))
(def sender-email "sender@mail.com")
(def recipient-email "recipient@mail.com") 

(facts "Tags can be listed"
       (let [memory (atom {})]
         (-> (k/session test-app)
             (jh/sign-up "recipient")
             (jh/activate-account {:activation-id (jh/get-activation-id stores-m recipient-email)
                                   :email recipient-email}) 
             (jh/sign-in "recipient")
             (kh/remember memory :recipient-email kh/state-on-account-page->email)
             jh/sign-out

             (jh/sign-up "sender")
             (jh/activate-account {:activation-id (jh/get-activation-id stores-m sender-email)
                                   :email sender-email
                                   :stores-m stores-m})
             (jh/sign-in "sender")
             (kh/remember memory :sender-email kh/state-on-account-page->email)

             ;; visit the tags page and show that there is no tag
             (k/visit (routes/absolute-path :get-all-tags))
             (kc/check-page-is :get-all-tags ks/tags-page-body)
             (kc/selector-includes-content [:title] "Tags")
             (kc/selector-matches-count ks/tags-page--table-rows 0)

             ;; do a tagged transaction
             (k/visit (routes/absolute-path :get-transaction-form))
             (kc/check-and-fill-in ks/transaction-form--recipient recipient-email)
             (kc/check-and-fill-in ks/transaction-form--amount "10.0")
             (kc/check-and-fill-in ks/transaction-form--tags "dupe, dupe space-separated! 日本語")
             (kc/check-and-press ks/transaction-form--submit)
             (kc/check-and-follow-redirect "to confirm transaction")
             (kc/check-and-press ks/confirm-transaction-form--submit)
             (kc/check-and-follow-redirect "to sender's account page")
             (kc/check-page-is :account [ks/account-page-body] :email (kh/recall memory :sender-email))

             ;; visit the tags page and show that there are three tags
             (k/visit (routes/absolute-path :get-all-tags))
             (kc/check-page-is :get-all-tags ks/tags-page-body)
             (kc/selector-includes-content [:title] "Tags")
             (kc/selector-matches-count ks/tags-page--table-rows 3)
             (kc/selector-includes-content ks/tags-page--table-rows--amount "10")

             ;; visit the tag details page and verify the moved value
             (k/visit (routes/absolute-path :get-tag-details :name "dupe"))
             (kc/check-page-is :get-tag-details ks/tag-details-page-body :name "dupe")
             (kc/selector-includes-content ks/tag-details-page--moved-value "10")
             (kc/selector-includes-content ks/tag-details-page--transactions "1")
             (kc/selector-includes-content ks/tag-details-page--created-by sender-email)
             (kc/selector-parse-and-check-content {:selector ks/tag-details-page--created
                                                   :content (simple-time.core/now)
                                                   :parse-fn simple-time.core/parse
                                                   ;; Check that the tag-creation happened less than 1 sec before
                                                   :checkers-fn (fn [actual expected]
                                                                  (roughly (time/- actual expected) 1000))}))))
