(ns comhype.web
  "Create ring handler for frame objects."
  (:require [org.httpkit.server :as http]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [comhype.frame :as frame :refer [->Frame]]))

(defn view-effect-script
  [html-text]
  (format "document.body.innerHTML = '%s';" html-text))

(defn frame-handler
  [frame-state]
  (let [clients (atom #{})
        update-clients!
        (fn [v]
          (let [view-script (-> v frame/render view-effect-script)]
            (doall (map #(http/send! % view-script) @clients))))]

    (add-watch frame-state clients
               (fn [_ _ _old v]
                 (update-clients! v)))
    
    (fn [request]
      (if (http/websocket-handshake-check request)
        (http/as-channel
         request
         {:on-receive ;;;; Process command
          (fn [_ message]
            (let [[command & args] (json/read-str message)]
              (swap! frame-state #(frame/dispatch-command %1 (keyword command) args))))
          :on-open ;;;; Subscribe to view changes
          (fn [ch]
            (swap! clients conj ch)
            (http/send! ch (-> @frame-state frame/render view-effect-script)))
          :on-close ;;;; Disconnect
          (fn [ch _status]
            (swap! clients disj ch))})
        {:status  200
         :headers {"content-type" "text/html"}
         :body    (str
                   (-> @frame-state frame/render)
                   (slurp (io/resource "init.html")))}))))
