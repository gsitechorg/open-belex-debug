(ns belex-dbg.adapters
  (:require
   [re-frame.core :as re-frame]
   [belex-dbg.events :as events]))

(defn handle-app-event [event]
  (re-frame/dispatch [::events/push-app-event event]))

(defn handle-file-load [[file-path file-html]]
  (re-frame/dispatch [::events/store-file file-path file-html]))
