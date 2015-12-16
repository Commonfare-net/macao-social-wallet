(ns freecoin.handlers.debug
  (:require [liberator.core :as lc]
            [freecoin.params :as param]
            [freecoin.utils :as utils]))


(lc/defresource echo [request]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok (utils/pretty request)
  )


(lc/defresource version [request]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :exists? {::data {:Freecoin "D-CENT"
                    :version param/version
                    :license "AGPLv3"
                    :os-name (.. System getProperties (get "os.name"))
                    :os-version (.. System getProperties (get "os.version"))
                    :os-arch (.. System getProperties (get "os.arch"))}}
  :handle-ok (fn [ctx] (utils/pretty (::data ctx)))
  )

;; :handle-ok #(util/pretty (::data %))
;; :handle-create #(::data %))
