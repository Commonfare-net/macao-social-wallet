(ns freecoin.handlers.debug
  (:require [liberator.core :as lc]
            [freecoin.utils :as util]
            [freecoin.params :as param]))

(lc/defresource echo [request]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok (fn [ctx] (util/pretty ctx)))

(lc/defresource version
  :available-media-types ["text/html" "application/json"]
  :exists? {::data {:Freecoin "D-CENT"
                      :version param/version
                      :license "AGPLv3"
                      :os-name (.. System getProperties (get "os.name"))
                      :os-version (.. System getProperties (get "os.version"))
                      :os-arch (.. System getProperties (get "os.arch"))}}
  
  :handle-ok #(util/pretty (::data %))
  :handle-create #(::data %))
