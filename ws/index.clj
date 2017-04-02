;; gorilla-repl.fileformat = 1

;; **
;;; # Gorilloid REPL
;;;
;;; Small template to write an application based on Gorilla REPL
;;;
;; **

;; @@
(ns freecoinadmin.term
  (:require
   [clojure.string :as string]
   [clojure.data.json :as json]
   [clojure.contrib.humanize :refer :all]
   [freecoinadmin.core :refer :all :reload :true])
  (:use [gorilla-repl core table latex html]
        ))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=
