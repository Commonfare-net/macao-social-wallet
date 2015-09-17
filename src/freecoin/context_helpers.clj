(ns freecoin.context-helpers)

(defn context->signed-in-uid [ctx]
  (get-in ctx [:request :session :signed-in-uid]))

(defn context->cookie-data [ctx]
  (get-in ctx [:request :session :cookie-data]))

(defn context->params [ctx]
  (get-in ctx [:request :params]))
