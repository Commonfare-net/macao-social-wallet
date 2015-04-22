;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>
;; Gareth Rogers <grogers@thoughtworks.com>

;; This program is free software: you can redistribute it and/or modify
;; it under the terms of the GNU Affero General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.

;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU Affero General Public License for more details.

;; You should have received a copy of the GNU Affero General Public License
;; along with this program.  If not, see <http://www.gnu.org/licenses/>.

(ns freecoin.pages
  (:gen-class)
  (:require
   [clojure.pprint :as pp]
   [liberator.dev]
   [net.cgrand.enlive-html :as html]
   )
  )

(html/deftemplate generic "templates/content.html"
  [content]
  [:head :title] (html/content "Freecoin")
  [:h2]          (html/html-content (:header content))
  [:h3]          (html/content      (:id     content))
  [:div#body]    (html/content      (:body   content))
  )

(html/deftemplate signup-template "templates/signup.html"
  [content]
  [:head :title] (html/content "Freecoin")
  [:h2]          (html/html-content (:header content))
  [:h3]          (html/content      (:id     content))
  [:div#body]    (html/content      (:body   content))
  )

(defn template [content] (reduce str (generic content)) )

(defn signup [content] (reduce str (signup-template content)) )
                       





;; (defn version
;;   "get the version of the running system"
;;   [md]
;;   ;; two mediatypes, just testing the possibility to filter
;;   (condp = (:type md)
;;     "html"
;;     (template {:header "Version"
;;                :body (str "Freecoin 0.2 running on "
;;                           (.. System getProperties (get "os.name"))
;;                           " version "
;;                           (.. System getProperties (get "os.version"))
;;                           " (" (.. System getProperties (get "os.arch")) ")"                          
;;                           "<h2>Cookies</h2>"
;;                           (:cookies md)
;;                           "<h2>Host machine details</h2>"
;;                           ;; blablabla
;;                           (clojure.string/replace
;;                            (slurp (java.io.FileReader. "/proc/cpuinfo"))
;;                            "\n" "<br>")
;;                           "</html>"
;;                           )})

;;     "txt"
;;     (str "Freecoin 0.1 running on "
;;          (.. System getProperties (get "os.name"))
;;          " version "
;;          (.. System getProperties (get "os.version"))
;;          " (" (.. System getProperties (get "os.arch")) ")"
;;          )
;;     )
;;   )
