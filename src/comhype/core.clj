(ns comhype.core
  (:require [org.httpkit.server :as http]
            [comhype.todos :as todos]
            [comhype.frame :as frame :refer [->Frame]]
            [comhype.web :as web]
            [mount.core :refer [defstate]]))

(defstate my-todos
  :start (atom (->Frame [{:item "Groceries" :done? false}
                         {:item "Taxes" :done? true}
                         {:item "Pick up drycleaning" :done false}
                         {:item "Destroy documents" :done true}]
                        todos/todo-list todos/todo-action)))

(defstate server
  :start (http/run-server
          (web/frame-handler my-todos)
          {:port 5055 :host "0.0.0.0" :join? false})
  :stop (server))

(do (mount.core/stop)
    (mount.core/start))

(comment
  
  (do @my-todos)
  
  (swap! my-todos #(frame/dispatch-command %1 ::todos/toggle-done [0]))
  
  (frame/render @my-todos)

  )
