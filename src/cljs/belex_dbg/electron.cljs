(ns belex-dbg.electron
  (:require
   [electron :refer [app BrowserWindow crashReporter]]
   [commander :refer [Command]]))

(def main-window (atom nil))

(defn opts->url [opts]
  (let [host (if (= opts.host "0.0.0.0")
               "localhost"
               opts.host)
        port opts.port]
    (str "http://" host ":" port)))

(defn init-browser [_ opts]
  (reset! main-window
          (BrowserWindow.
           #js {:show false
                :autoHideMenuBar true}))
  (.maximize @main-window)
  (.show @main-window)
  (.loadURL @main-window (opts->url opts))
  (.on @main-window "closed" #(reset! main-window nil)))

(def program
  (.. (Command.)
      (name "belex-dbg")
      (description "Belex debugger")
      (version "1.0rc8")
      (option "--host <string>"
              #_help  "Host (hostname or IP address) of the debug server."
              #_default "localhost")
      (option "--port <number>"
              #_help  "HTTP Port of the debug server"
              #_default 9803)
      (parse js/process.argv)))

(defn main []
  (.start crashReporter
          #js {:companyName "GSI Technology, Inc."
               :productName "belex-dbg"
               :submitURL "https://bitbucket.org/gsitech/belex-debug/issues"
               :autoSubmit false})
  (.on app "window-all-closed" #(when-not (= js/process.platform "darwin")
                                  (.quit app)))
  (.on app "ready" #(init-browser % (.opts program))))
