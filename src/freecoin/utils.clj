(ns freecoin.utils
  (:require
   [liberator.core :refer [resource defresource]]
;   [clojure.pprint :as pp]
   )
  )

(defmacro bench
  "Times the execution of forms, discarding their output and returning
  a long in nanoseconds."
  ([& forms]
   `(let [start# (System/nanoTime)]
      ~@forms
      (- (System/nanoTime) start#))))

;; name trace description
(defn log! [name trace desc]
  (println (format "LOG: %s (%s) %s" name trace desc))
  (liberator.core/log! name trace desc)
  )
