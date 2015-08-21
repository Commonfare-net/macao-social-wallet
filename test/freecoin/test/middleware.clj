(ns freecoin.test.middleware
  (:require [midje.sweet :refer :all]
            [freecoin.test.test-helper :as th]
            [freecoin.middleware :as m]))


(facts "about wrap-signed-in"
       (let [handler (fn [request] ...handled...)
             wrapped-handler (m/wrap-signed-in handler "/")]

         (fact "calls wrapped handler when user is signed in"
               (wrapped-handler {:session {:user-id ...user-id...}}) => ...handled...)

         (fact "redirects to provided route when user is not signed in"
               (wrapped-handler {}) => (th/check-redirects-to "/"))))
