(ns freecoin.journey.kerodon-selectors)

(def landing-page-body :.func--landing-page)
(def landing-page-error :.freecoin-error)
(def sign-in-link :.clj--sign-in-link)

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

(def tag-details-page-body :.func--tag-page--body)
(def tag-details-page--moved-value [:.func--tag-page--table :tbody :tr.func--tag-page--amount :td])
(def tag-details-page--transactions [:.func--tag-page--table :tbody :tr.func--tag-page--count :td])
(def tag-details-page--created-by [:.func--tag-page--table :tbody :tr.func--tag-page--created-by :td])
(def tag-details-page--created [:.func--tag-page--table :tbody :tr.func--tag-page--created :td])

(def auth-sign-in-body :.func--login-page--body)
(def auth-sign-up-form-first :.func--sign-up-first)
(def auth-sign-up-form-last :.func--sign-up-last)
(def auth-sign-up-form-email :.func--sign-up-email)
(def auth-sign-up-form-pswrd :.func--sign-up-pswrd)
(def auth-sign-up-form-conf-pswrd :.func--sign-up-conf-pswrd)
(def auth-sign-up-form-submit :.func--sign-up-submit)

(def email-confirmation-body :.func--email-confirmation--body)

(def auth-sign-in-form-email :.func--sign-in-email)
(def auth-sign-in-form-pswrd :.func--sign-in-pswrd)
(def auth-sign-in-form-submit :.func--sign-in-submit)

(def auth-form-problems :.form-problems)

(def auth-resend-form-email :.func--activation-email)
(def auth-resend-form-submit :.func--resend-email-submit)

(def account-activated-body :.func--account-activated--body)
