(ns freecoin.journey.tags
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [freecoin.journey.kerodon-selectors :as ks]
            [freecoin.journey.kerodon-checkers :as kc]
            [freecoin.journey.kerodon-helpers :as kh]
            [freecoin.test-helpers.integration :as ih]
            [freecoin.journey.helpers :as jh]
            [freecoin.db.storage :as s]
            [freecoin.blockchain :as blockchain]
            [freecoin.routes :as routes]
            [freecoin.config :as c]
            [freecoin.db.account :as account]))

(ih/setup-db)

(def stores-m (s/create-mongo-stores (ih/get-test-db)))
(def blockchain (blockchain/new-stub stores-m))

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
             (jh/activate-account (jh/get-activation-id stores-m recipient-email) recipient-email)
             (jh/sign-in "recipient")
             (kh/remember memory :recipient-email kh/state-on-account-page->email)
             jh/sign-out

             (jh/sign-up "sender")
             (jh/activate-account (jh/get-activation-id stores-m sender-email) sender-email)
             (jh/sign-in "sender")
             (kh/remember memory :sender-email kh/state-on-account-page->email)

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
             (kc/selector-includes-content ks/tags-page--table-rows--amount "10")

             ;; visit the tag details page and verify the moved value
             (k/visit (routes/absolute-path :get-tag-details :name "dupe"))
             (kc/check-page-is :get-tag-details ks/tag-details-page-body :name "dupe")
             (kc/selector-includes-content ks/tag-details-page--moved-value "10")
             (kc/selector-includes-content ks/tag-details-page--transactions "1"))))
