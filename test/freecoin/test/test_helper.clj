(ns freecoin.test.test-helper
  (:require [midje.sweet :as midje]
            [ring.mock.request :as request]
            [net.cgrand.enlive-html :as html]))

(defn create-request
  ([method path query-m]
   (create-request method path query-m {}))
  ([method path query-m session]
   (-> (request/request method path query-m)
       (assoc :params query-m)
       (assoc :session session))))

(defn authenticated-session [email]
  {:signed-in-email email})

(defn check-redirects-to [path]
  (midje/chatty-checker [response] (and
                             (#{302 303} (:status response))
                             (= (get-in response [:headers "Location"]) path))))

(defn check-response-status [status]
  (midje/chatty-checker [response] (and
                             (= status (:status response))
                             )))

(defn check-signed-in-as [email]
  (midje/chatty-checker [response] (= email (get-in response [:session :signed-in-email]))))

(def check-has-wallet-key
  (midje/contains {:session (midje/contains {:cookie-data midje/anything})}))

(defn enlive-m->attr [enlive-m selector attr]
  (-> enlive-m (html/select selector) first :attrs attr))

(defn enlive-m->text [enlive-m selector]
  (-> enlive-m (html/select selector) first html/text))

(defn text-is? [selector text]
  (midje/chatty-checker [enlive-m]
                        (= text (enlive-m->text enlive-m selector))))

(defn has-attr? [selector attr attr-val]
  (midje/chatty-checker [enlive-m]
                        (= attr-val (enlive-m->attr enlive-m selector attr))))

(defn has-form-action?
  ([path]
   (has-form-action? [:form] path))

  ([form-selector path]
   (has-attr? form-selector :action path)))

(defn has-form-method?
  ([method]
   (has-form-method? [:form] method))

  ([form-selector method]
   (has-attr? form-selector :method method)))

(defn links-to? [selector path]
  (has-attr? selector :href path))

(defn has-class? [selector css-class]
  (fn [enlive-m]
    ((midje/contains css-class) (enlive-m->attr enlive-m selector :class))))

(defn element-count [selector n]
  (fn [enlive-m]
    ((midje/n-of midje/anything n) (html/select enlive-m selector))))
