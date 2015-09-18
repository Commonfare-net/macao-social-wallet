(ns freecoin.journey.kerodon-checkers
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [kerodon.core :as k]
            [clojure.string :as string]
            [freecoin.routes :as r]))

(defn page-uri-is [state uri]
  (fact {:midje/name "Checking page uri:"}
        (-> state :request :uri) => uri)
  state)

(defn page-route-is [state scenic-action]
  (page-uri-is state (r/path scenic-action)))

(defn response-status-is [state status]
  (fact {:midje/name (str "Checking response status is " status)}
        (-> state :response :status) => status)
  state)

(defn selector-exists [state selector]
  (fact {:midje/name (str "Check element exists with " selector)}
        (-> state :enlive (html/select selector)) =not=> empty?)
  state)

(defn check-page-is [state route-action body-selector]
  (page-route-is state route-action)
  (response-status-is state 200)
  (selector-exists state body-selector))

(defn check-and-follow-redirect
  ([state description]
   "Possibly a double redirect"
   (fact {:midje/name (format "Attempting to follow redirect - %s" description)}
         (-> state :response :status) => 302)
   (try (k/follow-redirect state)
        (catch Exception state)))
  ([state]
   (check-and-follow-redirect state "")))
