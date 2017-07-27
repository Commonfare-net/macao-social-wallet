;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2017 Dyne.org foundation

;; Sourcecode designed, written and maintained by
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

(ns freecoin.test.handlers.qrcode
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as rmr]
            [freecoin-lib.db
             [mongo :as fm]
             [wallet :as w]]
            [freecoin-lib.core :as fb]
            [freecoin.handlers
             [qrcode :as handler]
             [transaction-form :as transaction-handler]]
            [net.cgrand.enlive-html :as html]
            [freecoin.test.test-helper :as th]
            [taoensso.timbre :as log])
  (:import [com.google.zxing BinaryBitmap
            MultiFormatReader]
           [com.google.zxing.client.j2se BufferedImageLuminanceSource]
           [com.google.zxing.common HybridBinarizer]))

(def user-email "user@mail.com")

(defn qrcode->text [byte-array-input-stream]
  (->> byte-array-input-stream
      (javax.imageio.ImageIO/read)
      (BufferedImageLuminanceSource.)
      (HybridBinarizer.)
      (BinaryBitmap.)
      (.decode (MultiFormatReader.))
      (.getText)))

(facts "Read qrcode and perform a send to"
       (fact "Requests the qr code for an email address"
             (let [wallet-store (fm/create-memory-store)
                   blockchain (fb/create-in-memory-blockchain :bk)
                   wallet (:wallet (w/new-empty-wallet! wallet-store blockchain
                                                        "name" user-email))
                   qrcode-handler (handler/qr-participant-sendto wallet-store)
                   response (qrcode-handler
                             (-> (rmr/request :get "/qrcode/")
                                 (assoc :params {:email user-email})
                                 (assoc-in [:session :signed-in-email] user-email)))]
               (:status response) => 200
               (-> (:body response) (type)) =>  java.io.ByteArrayInputStream

               (fact "Retrieves the URL from the QRCODE input stream"
                     (let [url (qrcode->text (:body response))]
                       (.contains url user-email) => truthy
                       (.contains url "send/to") => truthy

                       (fact "Performs a send to transaction based on the qrcode"
                             (let [transaction-handler (transaction-handler/get-transaction-to wallet-store)
                                   uri (java.net.URI. url) 
                                   transaction-response (-> (rmr/request :get "/send/to/")
                                                            (assoc-in [:params] {:email user-email})
                                                            (assoc-in [:session :signed-in-email] user-email)
                                                            (transaction-handler))]
                               (:status transaction-response) => 200
                               (-> (:body transaction-response) (html/html-snippet [:body])) => (th/has-form-method? "POST")
                               (-> (:body transaction-response) (html/html-snippet [:body])) => (th/has-form-action? (freecoin.routes/absolute-path :post-transaction-form))
                               (-> (:body transaction-response) (html/html-snippet [:body])) => (th/text-is? [:title] (str "Make a transaction -> " user-email)))))))))
