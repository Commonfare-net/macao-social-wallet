(ns freecoin.translation
  (:require
   [freecoin.params :as param]
   [clojure.tools.logging :as log]
   [clj-yaml.core :as yaml]
   [clojure.java.io :as io]
   ))

(defn load-translations-from-string [s]
  (yaml/parse-string s))

(defn load-translations-from-file [file-name]
  (-> file-name
      io/resource
      slurp
      load-translations-from-string))

(defn deep-merge
  "Recursively merges maps. If keys are not maps, the last value wins."
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(defn translation-map [lang]
  (deep-merge
   (load-translations-from-file (str "lang/en.yml"))
   (load-translations-from-file (str "lang/" lang ".yml"))
    )
  )


(defn locale [items]
  (get-in (translation-map (:language param/locale)) items)
  )
