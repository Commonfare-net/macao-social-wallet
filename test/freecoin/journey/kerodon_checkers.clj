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

(defn page-route-is [state scenic-action & route-params]
  (page-uri-is state (apply r/path scenic-action route-params)))

(defn response-status-is [state status]
  (fact {:midje/name (str "Checking response status is " status)}
        (-> state :response :status) => status)
  state)

(defn selector-exists [state selector]
  (fact {:midje/name (str "Check element exists with " selector)}
        (-> state :enlive (html/select selector)) =not=> empty?)
  state)

(defn check-page-is [state route-action body-selector & route-params]
  (apply page-route-is state route-action route-params)
  (response-status-is state 200)
  (selector-exists state body-selector))

(defn check-and-follow-redirect
  ([state description]
   "Possibly a double redirect"
   (fact {:midje/name (format "Attempting to follow redirect - %s" description)}
         (-> state :response :status) => (some-checker 302 303))
   (try (k/follow-redirect state)
        (catch Exception state)))
  ([state]
   (check-and-follow-redirect state "")))

(defn check-and-fill-in [state kerodon-selector value]
  (fact {:midje/name (format "Attempting to fill in input with selector: %s" kerodon-selector)}
        (let [enlive-selector [kerodon-selector]]
          (-> state :enlive (html/select enlive-selector) first :tag) => :input))
  (try (k/fill-in state kerodon-selector value)
       (catch Exception state)))

(defn check-and-press [state kerodon-selector]
  (fact {:midje/name (format "Attempting to press submit button with selector: %s" kerodon-selector)}
        (let [enlive-selector [kerodon-selector]]
          (-> state :enlive (html/select enlive-selector) first :attrs :type) => "submit"))
  (try (k/press state kerodon-selector)
       (catch Exception state)))

(defn selector-includes-content [state selector content]
  (fact {:midje/name "Check if element contains string"}
        (-> state :enlive (html/select selector) first html/text) => (contains content))
  state)

