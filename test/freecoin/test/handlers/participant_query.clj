(ns freecoin.test.handlers.participant-query
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [ring.mock.request :as rmr]
            [freecoin.db.mongo :as fm]
            [freecoin.db.wallet :as w]
            [freecoin.blockchain :as fb]
            [freecoin.translation :as t]
            [freecoin.test.test-helper :as th]
            [freecoin.handlers.participants :as fp]))

(defn create-wallet [wallet-store blockchain wallet-data]
  (let [{:keys [sso-id name email]} wallet-data]
    (:wallet (w/new-empty-wallet! wallet-store blockchain sso-id name email))))

(defn index->wallet-data [index]
  (let [sso-id (str "sso-id-" index)
        name (str "name-" index)
        email (str "wallet-" index "@email.com")]
    {:sso-id sso-id :name name :email email}))

(facts "about the account page"

       (fact "displays the signed-in participant's balance"
             (let [wallet-store (fm/create-memory-store)
                   blockchain (fb/create-in-memory-blockchain :bk)
                   wallet (:wallet (w/new-empty-wallet! wallet-store blockchain
                                                        "stonecutter-user-id" "name" "test@email.com"))
                   account-page-handler (fp/account wallet-store blockchain)
                   response (account-page-handler (-> (rmr/request :get "/account/")
                                                      (assoc :params {:email (:email wallet)})
                                                      (assoc :session {:signed-in-email (:email wallet)})))]
               (:status response) => 200
               (:body response) => (contains (t/locale [:wallet :balance]))))

       (fact "displays the balance of the participant wallet with the given email"
             (let [wallet-store (fm/create-memory-store)
                   blockchain (fb/create-in-memory-blockchain :bk)
                   my-wallet (:wallet (w/new-empty-wallet! wallet-store blockchain 
                                                           "stonecutter-user-id" "name" "test@email.com"))
                   her-wallet (:wallet (w/new-empty-wallet! wallet-store blockchain 
                                                            "stonecutter-user-id" "alice" "alice@email.com"))
                   account-page-handler (fp/account wallet-store blockchain)
                   response (account-page-handler (-> (rmr/request :get "/account/")
                                                      (assoc :params {:email (:email her-wallet)})
                                                      (assoc :session {:signed-in-email (:email my-wallet)})))]
               (:status response) => 200
               (:body response) => (contains (t/locale [:wallet :balance]))))

       (fact "gives a 404 when the requested email doesn't map to an existing wallet"
             (let [wallet-store (fm/create-memory-store)
                   blockchain (fb/create-in-memory-blockchain :bk)
                   my-wallet (:wallet (w/new-empty-wallet! wallet-store blockchain 
                                                           "stonecutter-user-id" "name" "test@email.com"))
                   account-page-handler (fp/account wallet-store blockchain)
                   response (account-page-handler (-> (rmr/request :get "/account/")
                                                      (assoc :params {:email "test@mail.com"})
                                                      (assoc :session {:signed-in-email (:email my-wallet)})))]
               (:status response) => 404)))

;;        (fact "can not be accessed when user is not signed in"))

(facts "about the participant query form"
       (fact "can be accessed by signed-in users"
             (let [wallet-store (fm/create-memory-store)
                   blockchain (fb/create-in-memory-blockchain :bk)
                   wallet (:wallet (w/new-empty-wallet! wallet-store blockchain 
                                                        "stonecutter-user-id" "name" "test@email.com"))
                   query-form-handler (fp/query-form wallet-store)
                   response (-> (th/create-request
                                 :get "/participants-query"
                                 {} (th/authenticated-session (:email wallet)))
                                query-form-handler)]
               (:status response) => 200))

       (fact "can not be accessed when user is not signed in"
             (let [wallet-store (fm/create-memory-store)
                   query-form-handler (fp/query-form wallet-store)
                   response (-> (th/create-request :get "/participants-query" {})
                                query-form-handler)]

               (:status response) => 401)))

(facts "about the participants query handler"
       (fact "can not be accessed when user is not signed in"
             (let [wallet-store (fm/create-memory-store)
                   participants-handler (fp/participants wallet-store)
                   response (-> (th/create-request :get "/participants" {})
                                participants-handler)]
               (:status response) => 401))

       (fact "without any query parameters, lists all participants"
             (let [wallet-store (fm/create-memory-store)
                   blockchain (fb/create-in-memory-blockchain :bk)
                   wallets (doall (->> (range 5)
                                       (map index->wallet-data)
                                       (map (partial create-wallet wallet-store blockchain))))
                   participants-handler (fp/participants wallet-store)
                   response (-> (th/create-request
                                 :get "/participants"
                                 {} (th/authenticated-session (:email (first wallets))))
                                participants-handler)]
               (:status response) => 200
               (-> (:body response) html/html-snippet) => (th/element-count [:.clj--participant__item] 5)))

       (tabular

        (fact "can query by name or email"
              (let [wallet-store (fm/create-memory-store)
                    blockchain (fb/create-in-memory-blockchain :bk)
                    wallet-data [{:name "James Jones" :email "james@jones.com" :sso-id "sso-id-1"}
                                 {:name "James Jones" :email "jim@jones.com" :sso-id "sso-id-2"}
                                 {:name "Sarah Lastname" :email "sarah@email.com" :sso-id "sso-id-3"}]
                    wallets (doall (map (partial create-wallet wallet-store blockchain) wallet-data))
                    participants-handler (fp/participants wallet-store)
                    query-m {:field ?field :value ?value}
                    response (-> (th/create-request
                                  :get "/participants"
                                  query-m (th/authenticated-session (:email (first wallets))))
                                 participants-handler)]
                (-> (:body response) html/html-snippet)
                => (th/element-count [:.clj--participant__item] ?expected-count)))

        ?field   ?value                  ?expected-count
        "name"   "James Jones"           2
        "email"  "james@jones.com"       1
        "fake"   "whatever"              0))

(future-facts "Query parameters are validated")
