(ns freecoin.translation
  (:require
   [clojure.tools.logging :as log]
   [clj-yaml.core :as yaml]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [environ.core :as env]))

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

(def translation
    (deep-merge
     (load-translations-from-file (env/env :translation-fallback))
     (load-translations-from-file (env/env :translation-language))))

(defn locale [items] (get-in translation items))
