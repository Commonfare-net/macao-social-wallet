;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2017 Dyne.org foundation

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>
;; Aspasia Beneti <aspra@dyne.org>

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

(ns freecoin.handlers.qrcode
  (:require [liberator.core :as lc]
            [freecoin.config :as config]
            [freecoin.auth :as auth]
            [freecoin.db.wallet :as wallet]
            [freecoin.context-helpers :as ch]
            [clj.qrgen :as qr]
            [taoensso.timbre :as log]))

(lc/defresource qr-participant-sendto [wallet-store]
  :allowed-methods [:get]

  :available-media-types ["image/png"]

  :authorized? #(auth/is-signed-in %)

  :exists? #(auth/has-wallet % wallet-store)

  ;; final endpoint rendered here (image served from url)
  :handle-ok
  (fn [ctx]
    (if-let [email (get-in ctx [:request :params :email])]
      (let [base-url (-> (config/create-config) config/base-url)]
        (qr/as-input-stream
         (qr/from (str base-url "/send/to/" email)
                  :charset "ISO-8859-1"))))))
