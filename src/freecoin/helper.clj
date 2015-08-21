(ns freecoin.helper)

;; TODO: How should a signed in session be identified? At the moment,
;; it's whether or not the session contains a :user-id field.
(defn signed-in? [request]
  (boolean (get-in request [:session :user-id])))
