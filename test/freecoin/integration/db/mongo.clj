(ns freecoin.integration.db.mongo
  (:require [midje.sweet :refer :all]
            [monger.core :as mongo]
            [monger.collection :as mongoc]
            [freecoin.db.mongo :as m]))

(def test-db "freecoin-test")
(def coll "test-coll")

(def db (atom nil))
(def conn (atom nil))

(defn connect []
  (reset! conn (mongo/connect))
  (reset! db (mongo/get-db @conn test-db)))

(defn clear-data []
  (mongo/drop-db @conn test-db)
  (reset! db (mongo/get-db @conn test-db)))

(defn disconnect []
  (mongo/disconnect @conn))

(defn create-empty-test-store [db]
  (clear-data)
  (m/create-mongo-store db coll))

;; SETUP
(connect)

(defn run-store-and-query-tests [store type]
  (facts {:midje/name (format "can store records, then query with a query map for %s store" type)}
         (let [record-1 {:field-1 "r1f1" :field-2 "r1f2,r2f2" :field-3 "r1f3,r3f3"}
               record-2 {:field-1 "r2f1" :field-2 "r1f2,r2f2" :field-3 "r2f3"}
               record-3 {:field-1 "r3f1" :field-2 "r3f2" :field-3 "r1f3,r3f3"}]
           (m/store! store :field-1 record-1)
           (m/store! store :field-1 record-2)
           (m/store! store :field-1 record-3)
           (tabular
            (fact "query returns correct result"
                  (set (m/query store ?query)) => (set ?result))
            ?query                                       ?result
            {:field-2 "r1f2,r2f2"}                       [record-1 record-2]
            {:field-1 "r1f1"}                            [record-1]
            {:field-2 "r1f2,r2f2" :field-3 "r1f3,r3f3"}  [record-1]
            {:field-2 "haven't got one"}                 []))))

(facts "run store and query tests for both in-memory and mongo stores"
       (run-store-and-query-tests (m/create-memory-store) "in-memory")
       (run-store-and-query-tests (create-empty-test-store @db) "mongo"))
