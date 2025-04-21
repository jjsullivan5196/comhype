(ns comhype.todos
  "Simple Todo App."
  (:require [hiccup2.core :as h]
            [comhype.frame :as frame :refer [->Frame]]))

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
                  :onclick (format "event.preventDefault();cmd('[\"comhype.todos/toggle-done\", %d]');" index)}]]])
        [:tr
         [:td [:input {:id "add-todo-item" :type "text"}]]
         [:td [:input {:type "button"
                       :value "Add"
                       :onclick "{ var item = document.getElementById(\"add-todo-item\").value; if (item) cmd(JSON.stringify([\"comhype.todos/add\", item])); }"}]]]]]
      h/html
      str))

(defmulti todo-action
  (fn [name & _args]
    name))

(defmethod todo-action ::toggle-done
  [_name todos index]
  (update-in todos [index :done?] not))

(defmethod todo-action ::add
  [_name todos item]
  (conj todos {:item item :done? false}))

(defmethod todo-action :default
  [_name todos & _]
  todos)

(comment
  (-> (->Frame [{:item "Groceries" :done? false}
                {:item "Taxes" :done? true}
                {:item "Pick up drycleaning" :done false}
                {:item "Destroy documents" :done true}]
               todo-list todo-action)
      (frame/dispatch-command ::toggle-done [0])
      frame/render)

  )
