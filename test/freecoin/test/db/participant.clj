(ns freecoin.test.db.participant
  (:require [midje.sweet :refer :all]
            [freecoin.db.uuid :as uuid]
            [freecoin.db.mongo :as fm]
            [freecoin.db.participant :as participant]))

(def wallet {})

(facts "can store then retrieve a participant"
       (against-background (uuid/uuid) => "a-uuid")
       (let [participant-store (fm/create-memory-store)]
         (fact "can store a participant"
               (participant/store! participant-store
                                               "sso-id" "Fred" "test@email.com" wallet)
               => (just {:uid "a-uuid"
                         :sso-id "sso-id"
                         :email "test@email.com"
                         :name "Fred"
                         :wallet wallet}))
         
         (fact "can fetch participant"
               (participant/fetch participant-store "a-uuid")
               => (just {:uid "a-uuid"
                         :sso-id "sso-id"
                         :email "test@email.com"
                         :name "Fred"
                         :wallet wallet}))

         (fact "can retrieve participant by sso-id"
               (participant/fetch-by-sso-id participant-store "sso-id")
               => (just {:uid "a-uuid"
                         :sso-id "sso-id"
                         :email "test@email.com"
                         :name "Fred"
                         :wallet wallet}))))
