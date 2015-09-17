(ns freecoin.test-helpers.store
  (:import [freecoin.db.mongo MemoryStore]))

(defprotocol TestStore
  (entry-count [s]
    "Total number of entries in the store"))

(extend-protocol TestStore
  MemoryStore
  (entry-count [this] (count @(:data this))))

