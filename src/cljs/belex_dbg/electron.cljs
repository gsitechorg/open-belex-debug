(ns belex-dbg.electron
  (:require [electron :refer [app BrowserWindow crashReporter]]))

(def main-window (atom nil))

(defn init-browser []
  (reset! main-window (BrowserWindow. #js {:show false}))
  (.maximize @main-window)
  (.show @main-window)
  (.loadURL @main-window "http://localhost:9803")
  (.on @main-window "closed" #(reset! main-window nil)))

(defn main []
  (.start crashReporter
          #js {:companyName "GSI Technology, Inc."
               :productName "belex-dbg"
               :submitURL "https://bitbucket.org/gsitech/belex-debug/issues"
               :autoSubmit false})
  (.on app "window-all-closed" #(when-not (= js/process.platform "darwin")
                                  (.quit app)))
  (.on app "ready" init-browser))
