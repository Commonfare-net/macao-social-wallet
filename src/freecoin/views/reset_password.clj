(ns freecoin.views.reset-password
  (:require [freecoin.form_helpers :as fh]
            [freecoin.translation :as t]
            [freecoin.routes :as routes]))

(defn reset-password-form [email recovery-id]
  {:renderer :bootstrap3-stacked
   :fields [{:name :new-password :type :password :class "func--reset-pswrd-new"}
            {:name :repeat-password :type :password :class "func--reset-pswrd-repeat"}
            {:name :submit :type :submit :class "func--reset-pswrd-submit"}]
   :validations [[:required [:new-password :repeat-password]]
                 [:matches #"^[^\n ]{8,}$" :new-password "The password should be Minimum 8 characters"]
                 [:equal [:new-password :repeat-password] "The confirmation password has to be the same as the password"]]
   :action (routes/absolute-path :reset-password-form :email email :password-recovery-id recovery-id)
   :method "post"})

(defn build [context]
  {:title (t/locale [:reset-password :title])
   :heading (t/locale [:reset-password :heading])
   :body-class "func--reset-password--body"
   :body [:div (let [{:keys [password-recovery-id email]} (get-in context [:request :params])]
                 (fh/render-form (reset-password-form email password-recovery-id) (:request context)))]})
