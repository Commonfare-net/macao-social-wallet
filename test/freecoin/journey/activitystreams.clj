(ns freecoin.journey.activitystreams
  (:require [cheshire.core :as cheshire]
            [clojure.tools.logging :as log]
            [freecoin.blockchain :as blockchain]
            [freecoin.config :as c]
            [freecoin.db.storage :as s]
            [freecoin.journey.kerodon-checkers :as kc]
            [freecoin.journey.kerodon-helpers :as kh]
            [freecoin.journey.kerodon-selectors :as ks]
            [freecoin.routes :as routes]
            [freecoin.test-helpers.integration :as ih]
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

(defn sign-up [state auth-code]
  (-> state
      (k/visit (str (routes/absolute-path (c/create-config) :sso-callback) "?code=" auth-code))
      (kc/check-and-follow-redirect "to account page")))

(def sign-in sign-up)

(defn sign-out [state]
  (k/visit state (routes/absolute-path (c/create-config) :sign-out)))

(defn test-activity [from-name amount to-name]
  {"@context"  "https://www.w3.org/ns/activitystreams"
   "@type"     "Transaction"
   "published" (time/format (time/datetime))
   "actor"     {"@type"       "Person"
                "displayName" from-name}
   "object"    {"@type"       "STUB"
                "displayName" (str amount " -> " to-name)}})

(defn no-timestamps [activity] (dissoc activity "published"))

(defn assert-timeless-activitystream [state contents]
  (fact {:midje/name "Checking JSON contents:"}
        (let [output (-> state :response :body cheshire/parse-string vec)]
          (map no-timestamps output) => (map no-timestamps contents)))
  state)


(facts "Activitystreams can be consumed"
       (let [memory (atom {})]
         (-> (k/session test-app)

             (sign-up "recipient")
             (kh/remember memory :recipient-uid kh/state-on-account-page->uid)
             sign-out

             (sign-up "sender")
             (kh/remember memory :sender-uid kh/state-on-account-page->uid)

             ;; do a transaction
             (k/visit (routes/absolute-path (c/create-config) :get-transaction-form))
             (kc/check-and-fill-in ks/transaction-form--recipient "recipient")
             (kc/check-and-fill-in ks/transaction-form--amount "10.0")
             (kc/check-and-press ks/transaction-form--submit)

             (kc/check-and-follow-redirect "to confirm transaction")
             (kc/check-and-press ks/confirm-transaction-form--submit)

             (kc/check-and-follow-redirect "to sender's account page")

             ;; visit the activitystreams page
             (k/visit (routes/absolute-path (c/create-config) :get-activity-streams))
             (kc/check-page-is-json :get-activity-streams)

             ;; TODO: fix the activitystreams content type
             ;; #_(kc/content-type-is "application/activity+json;charset=utf-8")

             (assert-timeless-activitystream [(test-activity "sender" 10 "recipient")])

             )))

