(ns freecoin.auth
  (:use midje.sweet)
  (:require
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [liberator.core :refer [resource defresource]]
   [liberator.dev]
;   [freecoin.secretshare :as ssss]
   [freecoin.db :as db]
   [freecoin.utils :as util]

   )
  )

(def token {}) ; { :uid :val }

;; security measure to truncate strings
(defn trunc [s n] (subs s 0 (min (count s) n)))

(defn render-slice [tuple which]
  {:pre [(contains? tuple :header)
         (contains? tuple :hi)
         (contains? tuple :lo)
         (coll? which)]
   :post [(= (count which) (count %))]}
  "takes a double vector from secretshare's `new-tuple`
extracts the slices at `which` height, return a new vector"

  (loop [c 1
         lo (first (:lo tuple))
         hi (first (:hi tuple))
         res [] ]
    (if (< c (count (:lo tuple)))
      (recur (inc c) (nth (:lo tuple) c) (nth (:hi tuple) c)
             (merge res (if (= c (some #{c} which))
                          (format "FXC1_%s_FXC_%s" lo hi))))
      (util/compress (merge res (if (= c (some #{c} which))
                                  (format "FXC1_%s_FXC_%s" lo hi)))))
    )
  )
(fact "Slicing the secret bread"
      (util/log! 'ACK 'render-slice (render-slice (ssss/new-tuple ssss/config) [1 3 5]))
      )

(defn parse-secret [in]
  (try

    (def token {})

    (if (empty? in)
      (throw (Exception. "Empty auth token"))

      (let [token (str/split (trunc in 128) #";")]

        (if-not (= (subs (first token) 0 3) "FXC")
          (throw (Exception.
                  (format "Invalid auth token: %s" (first token))
                  ))

          (let [part (str/split (first token) #"=")]

            (if (empty? (second part))
              (throw (Exception. "Missing value in auth token"))

              (def token {:uid (trunc (first part)  64)
                          :val (trunc (second part) 64)})
              )
            )
          )
        )
      )


    (catch Exception e
      (let [error (.getMessage e)]
        (liberator.core/log! "Error" "(auth/parse-secret)" error)
        ))

    (finally
      (if (empty? (:val token))
        (let [] (liberator.core/log! "Auth" "parse-secret" false) false)
        (let [] (liberator.core/log! "Auth" "parse-secret" true)  true)
        )
      )
    )
  )
