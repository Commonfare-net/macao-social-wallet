(ns freecoin.journey.tags
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [stonecutter-oauth.client :as soc]
            [freecoin.journey.kerodon-selectors :as ks]
            [freecoin.journey.kerodon-checkers :as kc]
            [freecoin.journey.kerodon-helpers :as kh]
            [freecoin.test-helpers.integration :as ih]
            [freecoin.journey.helpers :as jh]
            [freecoin.db.storage :as s]
            [freecoin.blockchain :as blockchain]
            [freecoin.routes :as routes]
            [freecoin.config :as c]))

(ih/setup-db)

(def stores-m (s/create-mongo-stores (ih/get-test-db)))
(def blockchain (blockchain/new-stub (ih/get-test-db)))

(background
 (soc/request-access-token! anything "sender") => {:user-info {:sub "sender"
                                                               :email "sender@email.com"}}
 (soc/request-access-token! anything "recipient") => {:user-info {:sub "recipient"
                                                                  :email "recipient@email.com"}})

(def test-app (ih/build-app {:stores-m stores-m
                             :blockchain blockchain}))

(facts "Tags can be listed"
       (let [memory (atom {})]
         (-> (k/session test-app)

             (jh/sign-up "recipient")
             (kh/remember memory :recipient-email kh/state-on-account-page->uid)
             jh/sign-out

             (jh/sign-up "sender")
             (kh/remember memory :sender-email kh/state-on-account-page->uid)

             ;; visit the tags page and show that there is no tag
             (k/visit (routes/absolute-path :get-all-tags))
             (kc/check-page-is :get-all-tags ks/tags-page-body)
             (kc/selector-includes-content [:title] "Tags")
             (kc/selector-matches-count ks/tags-page--table-rows 0)

             ;; do a tagged transaction
             (k/visit (routes/absolute-path :get-transaction-form))
             (kc/check-and-fill-in ks/transaction-form--recipient "recipient")
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
             (kc/selector-includes-content ks/tags-page--table-rows--amount "10"))))
