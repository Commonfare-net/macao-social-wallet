(ns freecoin.auth
  (:use midje.sweet)
  (:require
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [liberator.core :refer [resource defresource]]
   [liberator.dev]
   [freecoin.secretshare :as ssss]
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

    (if (empty? in)
      (throw (Exception. "Empty auth token"))
      (let [token (str/split (trunc in 128) #";")]
        (if-not (= (subs (first token) 0 4) "FXC_")
          (throw (Exception.
                  (format "Invalid auth token: %s" (first token))))
          (let [part (str/split (first token) #"=")]
            (if (empty? (second part))
              (throw (Exception. "Missing value in auth token"))
              {:_id (trunc (first part)  64)
               :secret (trunc (second part) 64)
               :valid true})
            )
          )
        )
      )

    (catch Exception e
      (let [error (.getMessage e)]
        (util/log! 'ERR 'auth/parse-secret error)
        ))

    (finally
      (util/log! 'Auth 'parse-secret in)
      )

    )
  )

(fact "Parsing secrets"
      (let [good-ex "FXC_38ea0686-9d36-416f-ae08-8f763c31f22f=FXC1_MLM7EWG7NV7JL_FXC_898589G3LR3KE;"
            bad-ex "FXC_38ea0686-9d36-416f-ae08-8f763c31f22f="]
        (parse-secret good-ex) => {:_id "FXC_38ea0686-9d36-416f-ae08-8f763c31f22f"
                                   :secret "FXC1_MLM7EWG7NV7JL_FXC_898589G3LR3KE"
                                   :valid true}
        (parse-secret bad-ex) => nil

        )
      )
