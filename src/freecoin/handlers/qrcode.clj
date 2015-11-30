(ns freecoin.handlers.qrcode
  (:require [liberator.core :as lc]
            [freecoin.params :as param]
            [freecoin.views :as fv]
            [freecoin.utils :as utils]
            [freecoin.auth :as auth]
            [freecoin.context-helpers :as ch]
            [clj.qrgen :as qr]))


(lc/defresource qr-participant-sendto
  :allowed-methods [:get]
  :available-media-types ["image/png"]
  ;; :authorized?           (:result (auth/check request))
  ;; :handle-unauthorized   (:problem (auth/check request))

  :handle-ok
  (fn [ctx]
    (if-let [uid (ch/context->signed-in-uid ctx)]
      (qr/as-input-stream
       (qr/from (format "http://%s:%d/send/to/%s"
                              (:address param/host)
                              (:port param/host)
                              uid)))
            ))
  )

      ;; else a name is specified
      ;; (let [wallet (first (storage/find-by-key
      ;;                      (:db (get-in request [:config :db-connection]))
      ;;                      "wallets" {:name (rc/percent-decode id)}))]

      ;;   (if (empty? wallet) ""
      ;;       (qr/as-input-stream
      ;;        (qr/from (format "http://%s:%d/send/to/%s"
      ;;                         (:address param/host)
      ;;                         (:port param/host)
      ;;                         (:name wallet))))
      ;;       ))
