(ns freecoin.journey.transactions
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [stonecutter-oauth.client :as soc]
            [freecoin.journey.kerodon-selectors :as ks]
            [freecoin.journey.kerodon-checkers :as kc]
            [freecoin.journey.kerodon-helpers :as kh]
            [freecoin.journey.helpers :as jh]
            [freecoin.test-helpers.integration :as ih]
            [freecoin.db.storage :as s]
            [freecoin.blockchain :as blockchain]
            [freecoin.routes :as routes]
            [freecoin.config :as c]))

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

(def sign-up jh/sign-up)

(def sign-in sign-up)

(defn sign-out [state]
  (k/visit state (routes/absolute-path :sign-out)))

(facts "Participant can send freecoins to another account"
       (let [memory (atom {})]
         (-> (k/session test-app)

             (sign-up "recipient")
             (kh/remember memory :recipient-email kh/state-on-account-page->email)
             sign-out

             (sign-up "sender")
             (kh/remember memory :sender-email kh/state-on-account-page->email)

             (k/visit (routes/absolute-path :get-all-transactions))
             (kh/remember memory :existing-tags (kh/body-selector-count [:select :option]))

             ;; visit the "all transactions page" should contain no transactions
             (k/visit (routes/absolute-path :get-all-transactions))
             (kc/check-page-is :get-all-transactions ks/transactions-page-body)
             (kc/selector-matches-count ks/transactions-page--table-rows 0)
             (kc/selector-includes-content [:title] "Transaction list")


             ;; visit the transactions page and show that there is no transaction
             (k/visit (routes/absolute-path :get-user-transactions :email (kh/recall memory :sender-email)))
             (kc/check-page-is :get-user-transactions ks/transactions-page-body :email (kh/recall memory :sender-email))
             (kc/selector-includes-content [:title] "Transaction list for sender")
             (kc/selector-matches-count ks/transactions-page--table-rows 0)

             ;; do a transaction
             (k/visit (routes/absolute-path :get-transaction-form))
             (kc/check-and-fill-in ks/transaction-form--recipient "recipient")
             (kc/check-and-fill-in ks/transaction-form--amount "10.0")
             (kc/check-and-fill-in ks/transaction-form--tags "dupe, dupe space-separated!")
             (kc/check-and-press ks/transaction-form--submit)

             (kc/check-and-follow-redirect "to confirm transaction")
             (kc/check-and-press ks/confirm-transaction-form--submit)

             (kc/check-and-follow-redirect "to sender's account page")
             (kc/check-page-is :account [ks/account-page-body] :email (kh/recall memory :sender-email))
             (kc/selector-includes-content [ks/account-page--balance] "-10")

             ;; visit the transactions page and show that there is now one transaction
             (k/visit (routes/absolute-path :get-user-transactions :email (kh/recall memory :sender-email)))
             (kc/check-page-is :get-user-transactions ks/transactions-page-body :email (kh/recall memory :sender-email))
             (kc/selector-matches-count ks/transactions-page--table-rows 1)
             (kc/selector-includes-content [:title] "Transaction list for sender")
             (kc/selector-matches-count [:span.tag :a] 2)

             (sign-in "recipient")
             (kc/check-page-is :account [ks/account-page-body] :email (kh/recall memory :recipient-email))
             (kc/selector-includes-content [ks/account-page--balance] "10")

             ;; visit the transactions page of the recipient and show that there is now one transaction as well
             (k/visit (routes/absolute-path :get-user-transactions :email (kh/recall memory :recipient-email)))
             (kc/check-page-is :get-user-transactions ks/transactions-page-body :email (kh/recall memory :recipient-email))
             (kc/selector-matches-count ks/transactions-page--table-rows 1)
             (kc/selector-includes-content [:title] "Transaction list for recipient")
             (kc/selector-matches-count [:select :option] 2)

             ;; visit the "all transactions page" should also contain a single transaction
             (k/visit (routes/absolute-path :get-all-transactions))
             (kc/check-page-is :get-all-transactions ks/transactions-page-body)
             (kc/selector-matches-count ks/transactions-page--table-rows 1)
             (kc/selector-includes-content [:title] "Transaction list")
             (kc/selector-matches-count [:select :option] (+ 2 (kh/recall memory :existing-tags))))))

(facts "Error messages show in form on invalid input"
       (let [memory (atom {})]
         (-> (k/session test-app)

             (sign-up "recipient")
             (kh/remember memory :recipient-email kh/state-on-account-page->email)
             sign-out

             (sign-up "sender")
             (kh/remember memory :sender-email kh/state-on-account-page->email)

             ;; required form fields
             (k/visit (routes/absolute-path :get-transaction-form))
             (kc/check-and-fill-in ks/transaction-form--recipient "")
             (kc/check-and-fill-in ks/transaction-form--amount "")
             (kc/check-and-press ks/transaction-form--submit)

             (kc/check-and-follow-redirect "back to form")
             (kc/check-page-is :get-transaction-form [ks/transaction-form--submit])
             (kc/selector-includes-content [ks/transaction-form--error-message] "Required field")

             ;; invalid recipient
             (kc/check-and-fill-in ks/transaction-form--recipient "non-existing-recipient")
             (kc/check-and-fill-in ks/transaction-form--amount "33")
             (kc/check-and-press ks/transaction-form--submit)

             (kc/check-and-follow-redirect "back to form")
             (kc/check-page-is :get-transaction-form [ks/transaction-form--submit])
             (kc/selector-includes-content [ks/transaction-form--error-message] "Recipient: Not found"))))


(facts "Participant can send freecoins to another account by entering PIN. First, PIN is removed from session by visiting 'forget PIN' URL"
       (let [memory (atom {})]
         (-> (k/session test-app)

             (sign-up "recipient")
             (kh/remember memory :recipient-email kh/state-on-account-page->email)
             sign-out

             (sign-up "sender")
             (kh/remember memory :sender-email kh/state-on-account-page->email)

             (k/visit (routes/absolute-path :get-all-transactions))
             (kh/remember memory :existing-tags (kh/body-selector-count [:select :option]))

             ;; visit /forget-secret to explicitly scrub the PIN from the session
             (k/visit (routes/absolute-path :forget-secret))
             (kc/check-and-follow-redirect "back to account page")
             (kc/check-page-is :account [ks/account-page-body] :email (kh/recall memory :sender-email))

             ;; now start a transaction
             (k/visit (routes/absolute-path :get-transaction-form))
             (kc/check-and-fill-in ks/transaction-form--recipient "recipient")
             (kc/check-and-fill-in ks/transaction-form--amount "10.0")
             (kc/check-and-press ks/transaction-form--submit)

             ;; we are on the confirm form; PIN entry is present
             (kc/check-and-follow-redirect "to confirm transaction")
             (kc/selector-matches-count [ks/confirm-transaction-form--secret] 1)
             (kc/check-and-fill-in ks/confirm-transaction-form--secret "asdf")
             (kc/check-and-press ks/confirm-transaction-form--submit)

             (kc/check-and-follow-redirect "to sender's account page")
             (kc/check-page-is :account [ks/account-page-body] :email (kh/recall memory :sender-email))

             ;; for next transactions, the PIN is again in the
             ;; session. Verify this by starting a transaction and
             ;; verifying that the PIN entry form on the confirmation
             ;; page is not there.
             (k/visit (routes/absolute-path :get-transaction-form))
             (kc/check-and-fill-in ks/transaction-form--recipient "recipient")
             (kc/check-and-fill-in ks/transaction-form--amount "10.0")
             (kc/check-and-press ks/transaction-form--submit)

             ;; we are on the confirm form; PIN entry is present
             (kc/check-and-follow-redirect "to confirm transaction")
             (kc/selector-matches-count [ks/confirm-transaction-form--secret] 0)

             (k/visit (routes/absolute-path :get-all-transactions))
             (kc/selector-matches-count [:select :option] (kh/recall memory :existing-tags)))))

(facts "Participants can filter transactions by tag"
       (let [memory (atom {})]
         (-> (k/session test-app)

             (sign-up "recipient")
             (kh/remember memory :recipient-email kh/state-on-account-page->email)
             sign-out

             (sign-up "sender")
             (kh/remember memory :sender-email kh/state-on-account-page->email)

             ;; remember the current transaction tags count
             (k/visit (routes/absolute-path :get-all-transactions))
                                        ;(kh/remember memory )
             (kh/remember memory :existing-tags (kh/body-selector-count [:select :option]))

             ;; do two transactions with overlapping tags:
             ;; tx one:
             (k/visit (routes/absolute-path :get-transaction-form))
             (kc/check-page-is :get-transaction-form ks/transactions-page-body)
             (kc/check-and-fill-in ks/transaction-form--recipient "recipient")
             (kc/check-and-fill-in ks/transaction-form--amount "10.0")
             (kc/check-and-fill-in ks/transaction-form--tags "tx-one tx-shared")
             (kc/check-and-press ks/transaction-form--submit)
             (kc/check-and-follow-redirect "to confirm first transaction")
             (kh/remember memory :confirmation-uid kh/state-on-account-page->email)
             (kc/check-page-is :get-confirm-transaction-form ks/confirm-page-body :confirmation-uid (kh/recall memory :confirmation-uid ))
             (kc/check-and-fill-in ks/confirm-transaction-form--secret "asdf")
             (kc/check-and-press ks/confirm-transaction-form--submit)
             (kc/check-and-follow-redirect "to sender's account page")
             (kc/check-page-is :account [ks/account-page-body] :email (kh/recall memory :sender-email))

             ;; tx two:
             (k/visit (routes/absolute-path :get-transaction-form))
             (kc/check-and-fill-in ks/transaction-form--recipient "recipient")
             (kc/check-and-fill-in ks/transaction-form--amount "10.0")
             (kc/check-and-fill-in ks/transaction-form--tags "tx-two tx-shared")
             (kc/check-and-press ks/transaction-form--submit)
             (kc/check-and-follow-redirect "to confirm second transaction")
             (kc/check-and-press ks/confirm-transaction-form--submit)
             (kc/check-and-follow-redirect "to sender's account page")
             (kc/check-page-is :account [ks/account-page-body] :email (kh/recall memory :sender-email))

             ;; do a third transaction with a completely new tag
             (k/visit (routes/absolute-path :get-transaction-form))
             (kc/check-and-fill-in ks/transaction-form--recipient "recipient")
             (kc/check-and-fill-in ks/transaction-form--amount "10.0")
             (kc/check-and-fill-in ks/transaction-form--tags "tx-three")
             (kc/check-and-press ks/transaction-form--submit)
             (kc/check-and-follow-redirect "to confirm third transaction")
             (kc/check-and-press ks/confirm-transaction-form--submit)
             (kc/check-and-follow-redirect "to sender's account page")
             (kc/check-page-is :account [ks/account-page-body] :email (kh/recall memory :sender-email))

             ;; visit the transactions page and verify that one transaction is found
             (k/visit (routes/absolute-path :get-all-transactions))

             (kc/selector-matches-count [:select :option] (+ 4 (kh/recall memory :existing-tags))))))
