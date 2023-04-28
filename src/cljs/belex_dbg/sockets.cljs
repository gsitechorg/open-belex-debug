(ns belex-dbg.sockets
  (:require
   [cognitect.transit :as t]
   ["socket.io" :as socket-io]
   ["rxjs" :as rxjs]))

(def socket (atom nil))

(def app-events (rxjs/Subject.))

(def file-loads (rxjs/Subject.))

(defn subscribe-to-app-event [handler]
  (.subscribe app-events #js {:next handler}))

(defn subscribe-to-file-load [handler]
  (.subscribe file-loads #js {:next handler}))

(defn poll-app-event []
  (when @socket
    (.emit @socket "poll_app_event")))

(defn restart-app []
  (when @socket
    (.emit @socket "restart")))

(defn transit->json [transit-data]
  (let [reader (t/reader :json)]
    (t/read reader transit-data)))

(defn handle-app-event [transit-event]
  (let [event (transit->json transit-event)]
    (.next app-events event)))

(defn handle-file-load [payload]
  (.next file-loads (transit->json payload)))

(defn load-file-source [file-path]
  (when @socket
    (.emit @socket "load_file" file-path)))

;; (defn handle-error [event]
;;   (js/console.log event))

(defn stop! []
  (when @socket
    (println "Disconnecting from debug server.")
    (.disconnect @socket)
    (reset! socket nil)))

(defn start! []
  (when-not @socket
    (println "Connecting to debug server.")
    (reset! socket (socket-io/io #js {:autoConnect false}))
    (.on @socket "disconnect" stop!)
    (.on @socket "app_event" handle-app-event)
    (.on @socket "file_load" handle-file-load)
    (.open @socket)))
