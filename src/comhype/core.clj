(ns comhype.core
  (:require [org.httpkit.server :as http]
            [hiccup2.core :as h]
            [clojure.data.json :as json]
            [clojure.java.io :as io]))

(def my-todos
  (atom [{:item "Groceries" :done? false}
         {:item "Taxes" :done? true}
         {:item "Pick up drycleaning" :done false}
         {:item "Destroy documents" :done true}]))

(defn todo-list
  [todos]
  (-> [:table
       [:thead [:tr [:th "Thing to do"] [:th "Done?"]]]
       [:tbody
        (for [[index {:keys [item done?]}] (map vector (range) todos)]
          [:tr
           [:td item]
           [:td [:input
                 {:type    "checkbox"
                  :checked done?
                  :onclick (format "event.preventDefault();cmd('[\"todos/toggle-done\", %d]');" index)}]]])
        [:tr
         [:td [:input {:id "add-todo-item" :type "text"}]]
         [:td [:input {:type "button"
                       :value "Add"
                       :onclick "{ var item = document.getElementById(\"add-todo-item\").value; if (item) cmd(JSON.stringify([\"todos/add\", item])); }"}]]]]]
      h/html
      str))

(defmulti todo-action
  (fn [name & _args]
    name))

(defmethod todo-action :todos/toggle-done
  [_name todos index]
  (update-in todos [index :done?] not))

(defmethod todo-action :todos/add
  [_name todos item]
  (conj todos {:item item :done? false}))

(defmethod todo-action :default
  [_name todos & _]
  todos)

(defn view-effect-script
  [html-text]
  (format "document.body.innerHTML = '%s';" html-text))

(defn app-handler
  "TODO a real API for this.

  As it stands, the high level steps are:

  1. Create storage for client objects
  2. Attach events for when application state changes
  3. Handle incoming client connections, send current view on connect
  4. Handle client commands, update model with command-fn"
  [state command-fn render-fn]
  (let [clients (atom #{})
        update-clients!
        (fn [v]
          (let [view-script (-> v render-fn view-effect-script)]
            (doall (map #(http/send! % view-script) @clients))))]

    (remove-watch state #'app-handler)
    (remove-watch render-fn #'app-handler)
    (add-watch state #'app-handler
               (fn [_ _ _old v]
                 (update-clients! v)))
    (add-watch render-fn #'app-handler
               (fn [& _]
                 (update-clients! @state)))
    
    (fn [request]
      (if (http/websocket-handshake-check request)
        (http/as-channel
         request
         {:on-receive ;;;; Process command
          (fn [_ message]
            (let [[command & args] (json/read-str message)]
              (swap! state #(apply command-fn (keyword command) %1 args))))
          :on-open ;;;; Subscribe to view changes
          (fn [ch]
            (swap! clients conj ch)
            (http/send! ch (-> @state render-fn view-effect-script)))
          :on-close ;;;; Disconnect
          (fn [ch _status]
            (swap! clients disj ch))})
        {:status  200
         :headers {"content-type" "text/html"}
         :body    (str
                   (-> @state render-fn)
                   (slurp (io/resource "init.html")))}))))

(def ^:dynamic *server*
  (http/run-server
   (app-handler my-todos #'todo-action #'todo-list)
   {:port 5055 :join? false}))

(comment
  (do @my-todos)

  (todo-action :app/toggle-done @my-todos 0)
  
  (todo-list @my-todos)

  ;; This stops the HTTP server
  (*server*)
  )
