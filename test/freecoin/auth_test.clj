(ns freecoin.auth-test
  (:use midje.sweet)
  (:require
   [freecoin.secretshare :as ssss]
   [freecoin.utils :as util]
   [freecoin.auth :as auth]
   [freecoin.db :as db]
   )
  )

(fact "Slicing the secret on 1, 3 5"
      (util/log! 'ACK 'render-slice (auth/render-slice (ssss/new-tuple ssss/config) [1 3 5]))
      )

(fact "Parsing the secret from a cookie"
      (let [good-ex "FXC_38ea0686-9d36-416f-ae08-8f763c31f22f=FXC1_MLM7EWG7NV7JL_FXC_898589G3LR3KE;"
            bad-ex "FXC_38ea0686-9d36-416f-ae08-8f763c31f22f="]
        (auth/parse-secret good-ex) => {:_id "FXC_38ea0686-9d36-416f-ae08-8f763c31f22f"
                                   :secret "FXC1_MLM7EWG7NV7JL_FXC_898589G3LR3KE"
                                   :valid true}
        (auth/parse-secret bad-ex) => nil

        )
      )

