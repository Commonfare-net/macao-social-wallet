(ns freecoin.random-test
  (:use midje.sweet)
  (:require [freecoin.random :as random]))

(fact "Checking random generator"
  (fact "proper size returned"
    (.length (:string (random/create 20 3.0))) => 20))

