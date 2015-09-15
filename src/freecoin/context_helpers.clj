(ns freecoin.context-helpers)

(defn context->signed-in-uid [ctx]
  (get-in ctx [:request :session :signed-in-uid]))

