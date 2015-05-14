(ns freecoin.auth
  (:require
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [liberator.core :refer [resource defresource]]
   [liberator.dev]
   [freecoin.secretshare :as ssss]
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
  "takes a double vector from secretshare's `new-tuple` extracts the slices at `which` height (collection of numbers indicating positions, then return a new vector"

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

(defn parse-secret [in]
  (try

    (if (empty? in)
      (throw (Exception. "Empty auth token"))
      (let [token (str/split (trunc in 128) #";")]
        (if-not (= (subs (first token) 0 4) "FXC1_")
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
      ;;  (util/log! 'Auth 'parse-secret in)
      )

    )
  )
