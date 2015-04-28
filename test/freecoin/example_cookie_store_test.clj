(ns freecoin.example-cookie-store-test
  (:require [midje.sweet :refer :all]
            [freecoin.example-cookie-store :refer [new-example-store]]))

(def my-super-secret-key "foo")

(fact "Writes data"
      (let [store (new-example-store my-super-secret-key)]
        (.write-session store nil {:some-key "some-value"}) => "encrypted-with-foo:{:some-key \"some-value\"}"))

(fact "Reads data"
      (let [store (new-example-store my-super-secret-key)]
        (.read-session store "encrypted-with-foo:{:some-key \"some-value\"}") => {:some-key "some-value"}))

(fact "Data can be empty when reading"
      (let [store (new-example-store my-super-secret-key)]
        (.read-session store nil) => {}))
