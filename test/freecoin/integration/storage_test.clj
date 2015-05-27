(ns freecoin.integration.storage-test
  (:require [midje.sweet :refer :all]
            [monger.collection :as mc]
            [freecoin.integration.storage-helpers :as sh]
            [freecoin.storage :as storage]))

(def db-connection (atom nil))

(def test-collection "test_collection")

(facts "About inserting and retrieving documents"
       (against-background
        [(before :contents (do (reset! db-connection (storage/connect sh/test-db-config))
                               (sh/drop-collection @db-connection test-collection)))
         (after :facts (sh/drop-collection @db-connection test-collection))
         (after :contents (swap! db-connection storage/disconnect))]

        (fact "Can insert and retrieve a document"
              (let [test-doc {:_id "doc-id"
                              :string-data "abc"
                              :number-data 1
                              :map-data {:a 1
                                         :b "xyz"}
                              :vector-data [1 2 "a" "b"]}
                    stored-document (storage/insert @db-connection test-collection test-doc)
                    retrieved-document (storage/find-by-id @db-connection test-collection (:_id test-doc))]
                stored-document => test-doc
                retrieved-document => test-doc))))
