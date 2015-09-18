(ns freecoin.views.participants-query-form
  (:require [formidable.core :as fc]
            [freecoin.routes :as routes]
            [freecoin.views :as fv]))

(def participants-form-spec
  {:fields [{:name :field
             :type :select
             :options ["name" "email"]}
            {:name :value :type :text}]
   :validations [[:required [:field :value]]]
   :action (routes/path :participants)
   :method "get"})

(defn build [_content]
  {:title "Find wallet"
   :heading "Search for a wallet"
   :body (fc/render-form participants-form-spec)})
