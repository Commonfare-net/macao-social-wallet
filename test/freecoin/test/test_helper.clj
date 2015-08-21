(ns freecoin.test.test-helper
  (:require [midje.sweet :as midje]
            [net.cgrand.enlive-html :as html]
            [freecoin.helper :as fh]))

(defn check-redirects-to [path]
  (midje/chatty-checker [response] (and
                             (= (:status response) 302)
                             (= (get-in response [:headers "Location"]) path))))

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
