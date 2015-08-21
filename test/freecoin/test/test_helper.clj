(ns freecoin.test.test-helper
  (:require [midje.sweet :as midje]
            [freecoin.helper :as fh]))

(defn check-redirects-to [path]
  (midje/checker [response] (and
                             (= (:status response) 302)
                             (= (get-in response [:headers "Location"]) path))))
