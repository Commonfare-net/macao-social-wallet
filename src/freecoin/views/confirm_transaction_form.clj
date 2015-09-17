(ns freecoin.views.confirm-transaction-form
  (:require [freecoin.views :as fv]
            [formidable.core :as fc]))

(defn build [_content]
  {:title "Confirm transaction"
   :heading "Please confirm to execute transaction"
   :body (fc/render-form {:submit-label "Confirm"})})
