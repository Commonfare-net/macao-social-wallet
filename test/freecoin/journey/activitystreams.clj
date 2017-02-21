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

(ns freecoin.journey.activitystreams
  (:require [cheshire.core :as cheshire]
            [taoensso.timbre :as log]
            [freecoin.blockchain :as blockchain]
            [freecoin.config :as c]
            [freecoin.db.storage :as s]
            [freecoin.journey.helpers :as h]
            [freecoin.journey.kerodon-checkers :as kc]
            [freecoin.journey.kerodon-helpers :as kh]
            [freecoin.journey.kerodon-selectors :as ks]
            [freecoin.routes :as routes]
            [freecoin.db.wallet :as wallet]
            [freecoin.test-helpers.integration :as ih]
            [environ.core :as env]
            [kerodon.core :as k]
            [midje.sweet :refer :all]
            [simple-time.core :as time]
            [stonecutter-oauth.client :as soc]))

(ih/setup-db)

(def stores-m (s/create-mongo-stores (ih/get-test-db)))
(def blockchain (blockchain/new-stub (ih/get-test-db)))

(def test-app (ih/build-app {:stores-m stores-m
                             :blockchain blockchain}))


(background
 (soc/request-access-token! anything "sender") => {:user-info {:sub "sender"
                                                               :email "sender@email.com"}}
 (soc/request-access-token! anything "recipient") => {:user-info {:sub "recipient"
                                                                  :email "recipient@email.com"}})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn render-activity [transaction]
  {"type"     "Transaction"
   "published" (str (time/format (time/datetime)) "Z")
   "actor"     {"type"       "Person"
                "displayName" (:from transaction)}
   "target"    {"type"       "Person"
                "displayName" (:to transaction)}
   "object"    {"type"       "STUB"
               "displayName" (str (:amount transaction))
               "url" (str (env/env :base-url) "/transactions/EMAIL")}
   })
  
(defn test-activity [transactions]
  {"@context"  "https://www.w3.org/ns/activitystreams"
   "type"      "Container"
   "name"      "Activity stream"
   "totalItems" (count transactions)
   "items"     (map #(render-activity %) transactions)})

(defn no-variables [activity]
  (-> activity
      (dissoc "items")
      )
      ;; (update-in ["object"] dissoc "url"))
  )

(defn assert-timeless-activitystream [state contents]
  (fact {:midje/name "Checking JSON contents:"}
        (let [output (-> state :response :body cheshire/parse-string vec)]
          (map no-variables output) => (map no-variables contents)))
  state)

(defn make-transaction [state blockchain from-email amount to-email params]
  (let [wallet-store (:wallet-store stores-m)
        from (wallet/fetch wallet-store from-email)
        to (wallet/fetch wallet-store to-email)]
    (blockchain/make-transaction blockchain (:account-id from) amount (:account-id to) params))
  state)


(defn check-page-is-activity-stream [state route-action & route-params]
  (apply kc/page-route-is state route-action route-params)
  (kc/response-status-is state 200)
  (kc/content-type-is state "application/activity+json")
  )


#(facts "Activitystreams can be consumed and are filled once transactions are done"
       (let [memory (atom {})]
         (ih/reset-db)
         (-> (k/session test-app)

             (h/sign-up "recipient")
             (kh/remember memory :recipient-email kh/state-on-account-page->uid)
             h/sign-out

             (h/sign-up "sender")
             (kh/remember memory :sender-email kh/state-on-account-page->uid)

             ;; do a transaction
             (k/visit (routes/absolute-path :get-transaction-form))
             (kc/check-and-fill-in ks/transaction-form--recipient "recipient")
             (kc/check-and-fill-in ks/transaction-form--amount "10")
             (kc/check-and-press ks/transaction-form--submit)

             (kc/check-and-follow-redirect "to confirm transaction")
             (kc/check-and-press ks/confirm-transaction-form--submit)

             (kc/check-and-follow-redirect "to sender's account page")

             ;; visit the activitystreams page
             (k/visit (routes/absolute-path :get-activity-streams))
             (check-page-is-activity-stream :get-activity-streams)

             (assert-timeless-activitystream [(test-activity "sender" 10 "recipient")])

             (k/visit (routes/absolute-path :get-activity-streams))
             (check-page-is-activity-stream :get-activity-streams)

             (kc/content-type-is "application/activity+json")

             )))

#(facts "Activitystreams JSON is empty on initial load"
       (let [memory (atom {})]
         (ih/reset-db)
         (-> (k/session test-app)

             ;; visit the activitystreams page
             (k/visit (routes/absolute-path :get-activity-streams))
             (check-page-is-activity-stream :get-activity-streams)
             (assert-timeless-activitystream [])
             )))


#(facts "Activitystreams JSON contains transaction log as soon as transactions are done on blockchain"
       (let [memory (atom {})]
         (ih/reset-db)
         (-> (k/session test-app)

             (h/sign-up "recipient")
             (kh/remember memory :recipient-email kh/state-on-account-page->uid)
             h/sign-out

             (h/sign-up "sender")
             (kh/remember memory :sender-email kh/state-on-account-page->uid)
             h/sign-out

             ;; do a few transactions
             (make-transaction blockchain (kh/recall memory :sender-email) 99 (kh/recall memory :recipient-email) {})
             (make-transaction blockchain (kh/recall memory :sender-email) 101 (kh/recall memory :recipient-email) {})

             ;; visit the activitystreams page
             (k/visit (routes/absolute-path :get-activity-streams))
             (check-page-is-activity-stream :get-activity-streams)

             (assert-timeless-activitystream
              [(test-activity [{:from "sender" :amount 101 :to "recipient"}
                               {:from "sender" :amount 99  :to "recipient"}])])
             )))


#(facts "Activitystreams JSON can be filtered on time with from/to parameters"
       (let [memory (atom {})]
         (ih/reset-db)
         (-> (k/session test-app)

             (h/sign-up "recipient")
             (kh/remember memory :recipient-email kh/state-on-account-page->uid)
             h/sign-out

             (h/sign-up "sender")
             (kh/remember memory :sender-email kh/state-on-account-page->uid)
             h/sign-out

             ;; do a few transactions
             (make-transaction blockchain (kh/recall memory :sender-email) 1 (kh/recall memory :recipient-email)
                               {:timestamp (time/datetime 2015 12 1)})
             (make-transaction blockchain (kh/recall memory :sender-email) 2 (kh/recall memory :recipient-email)
                               {:timestamp (time/datetime 2015 12 2)})
             (make-transaction blockchain (kh/recall memory :sender-email) 3 (kh/recall memory :recipient-email)
                               {:timestamp (time/datetime 2015 12 3)})

             ;; visit the activitystreams page
             (k/visit (routes/absolute-path :get-activity-streams))
             (assert-timeless-activitystream
              [(test-activity "sender" 3 "recipient")
               (test-activity "sender" 2 "recipient")
               (test-activity "sender" 1 "recipient")])

             ;; test 'from' parameter
             (k/visit (str (routes/absolute-path :get-activity-streams) "?from=2015-12-02"))
             (assert-timeless-activitystream
              [(test-activity "sender" 3 "recipient")
               (test-activity "sender" 2 "recipient")
               ])

             ;; test 'to' parameter
             (k/visit (str (routes/absolute-path :get-activity-streams) "?to=2015-12-02"))
             (assert-timeless-activitystream
              [(test-activity "sender" 1 "recipient")
               ])

             ;; test both 'from' and 'to' parameters
             (k/visit (str (routes/absolute-path :get-activity-streams) "?from=2015-12-02&to=2015-12-02"))
             ;; TODO: Check off-by-one / date boundaries on activitystream from/to parameters
             #_(assert-timeless-activitystream
                [(test-activity "sender" 2 "recipient")
                 ])

             )))
