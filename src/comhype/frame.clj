(ns comhype.frame
  "Defines the 'frame' interface.")

(defprotocol ICommandFrame
  "A CQRS-style receiver."
  (render [_] "Result of query for this frame.")
  (dispatch-command [_ name args] "Apply command to frame."))

(defrecord Frame [model render-fn command-fn]
  ICommandFrame
  (render [_]
    (render-fn model))
  (dispatch-command [self name args]
    (assoc self :model (apply command-fn name model args))))
