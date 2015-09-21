(ns freecoin.journey.kerodon-helpers)

(defn debug [state]
  (clojure.pprint/pprint state)
  state)
