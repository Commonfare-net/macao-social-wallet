(ns freecoin.auth
  (:require
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [liberator.core :refer [resource defresource]]
   [liberator.dev]
   [freecoin.secretshare :as ssss]
   )
  )


(def token {})
(defn get-token [] token)

(defn parse-secret [in]
  (try

    (def token {})
    
    (if (empty? in)
      (throw (Exception. "Empty auth token")))
    
    (let [token (str/split in #";")]
      
      (if-not (= (subs (first token) 0 3) "FXC")
        (throw (Exception. 
                (format "Invalid auth token: %s" (first token))
                )))
      
      (let [part (str/split (first token) #"=")]
        
        (if (empty? (second part))
          (throw (Exception. "Missing value in auth token")))
        
        (def token {:uid (first part)
                    :val (second part)})
        
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
