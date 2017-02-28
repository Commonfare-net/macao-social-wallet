(ns freecoin.context-helpers)

(defn context->signed-in-email [ctx]
  (get-in ctx [:request :session :signed-in-email]))

(defn context->cookie-data [ctx]
  (get-in ctx [:request :session :cookie-data]))

(defn context->params [ctx]
  (get-in ctx [:request :params]))
