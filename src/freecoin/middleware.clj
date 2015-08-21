(ns freecoin.middleware
  (:require [ring.util.response :as r]
            [freecoin.helper :as fh]))

(defn wrap-signed-in [handler sign-in-route]
  (fn [request]
    (if (fh/signed-in? request)
      (handler request)
      (r/redirect sign-in-route))))
