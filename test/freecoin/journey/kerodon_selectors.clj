(ns freecoin.journey.kerodon-selectors)

(def landing-page-body :.func--landing-page)

(def index-page-body :.func--index-page)

(def account-page-body :.func--account-page--body)
(def account-page--balance :.func--account-page--balance)

(def transaction-form--amount :.func--transaction-form--amount)
(def transaction-form--recipient :.func--transaction-form--recipient)
(def transaction-form--submit :.func--transaction-form--submit)
(def transaction-form--tags :.func--transaction-form--tags)
(def transaction-form--error-message :div.form-problems)

(def confirm-page-body :.func--confirmation-page--body)
(def confirm-transaction-form--submit :.func--confirm-transaction-form--submit)
(def confirm-transaction-form--secret :.func--confirm-transaction-form--secret)

(def transactions-page-body :.func--transactions-page--body)
(def transactions-page--table-rows [:.func--transactions-page--table :tbody :tr])

(def tags-page-body :.func--tags-page--body)
(def tags-page--table-rows [:.func--tags-page--table :tbody :tr])
(def tags-page--table-rows--amount [:.func--tags-page--table :tbody :*:last-child])
