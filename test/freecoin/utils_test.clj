(ns freecoin.utils-test
  (:require [freecoin.utils :as utils]
            [midje.sweet :as midje]))

(midje/tabular
 (midje/fact "Pagination uses :skip and :limit with consistent defaults"
             (utils/pagination ?params) => ?expected)
 ?params                ?expected
 {}                     {:skip 0  :limit 20}
 {:skip 99  :limit 1}   {:skip 99 :limit 1}
 {:skip nil :limit 42}  {:skip 0  :limit 42}
 {:skip 42  :limit nil} {:skip 42 :limit 20})
