(ns freecoin.test-helpers.store
  (:import [freecoin.db.mongo MemoryStore]
           [freecoin.blockchain InMemoryBlockchain]))

(defprotocol TestStore
  (entry-count [s]
    "Total number of entries in the store")
  (summary [s]
    "A map providing some summary information about the store"))

(extend-protocol TestStore
  MemoryStore
  (entry-count [this] (count @(:data this)))
  (summary [this] {:entry-count (count @(:data this))}))

(extend-protocol TestStore
  InMemoryBlockchain
  (entry-count [this] nil)
  (summary [this] {:transaction-count (count @(:transactions-atom this))}))

