(ns freecoin.test.translation
  (:require [midje.sweet :refer :all]
            [freecoin.translation :as trans]
            [clojure.tools.logging :as log]
  ))

(facts "Can load translations from a file"
       (trans/load-translations-from-file "test-translations.yml") => {:a {:first "primo" :second "secondo"}})

(facts "Translation has working fallback"
       (let [tmap (trans/deep-merge
                   (trans/load-translations-from-file "test-fallback.yml")
                   (trans/load-translations-from-file "test-translations.yml"))
             ]
         (get-in tmap [:a :first]) => "primo"
         (get-in tmap [:a :first]) =not=> "first"
         (get-in tmap [:a :third]) => "third"
         )

       (let [tmap (trans/deep-merge
                   (trans/load-translations-from-file "test-translations.yml")
                   (trans/load-translations-from-file "test-fallback.yml"))
             ]
         (get-in tmap [:a :first]) =not=> "primo"
         (get-in tmap [:a :first]) => "first"
         )

       )
