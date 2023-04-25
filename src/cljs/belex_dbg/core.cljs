(ns belex-dbg.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [breaking-point.core :as bp]
   [dommy.core :as dommy :refer-macros [sel1]]
   [belex-dbg.config :as config]
   [belex-dbg.adapters :as adapters]
   [belex-dbg.events :as events]
   [belex-dbg.sockets :as sockets]
   [belex-dbg.views :as views]))


(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (sel1 :#app)]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)))

(defn terminate []
  (sockets/stop!))

(defn init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (re-frame/dispatch-sync [::bp/set-breakpoints
                           {;; required
                            :breakpoints [:mobile
                                          768
                                          :tablet
                                          992
                                          :small-monitor
                                          1200
                                          :large-monitor]}])
  (sockets/subscribe-to-app-event adapters/handle-app-event)
  (sockets/subscribe-to-file-load adapters/handle-file-load)
  (re-frame/dispatch-sync [::events/start-socket])
  (dev-setup)
  (mount-root)
  (re-frame/dispatch-sync [::events/open-terminal])
  (re-frame/dispatch-sync [::events/poll-app-event])
  (js/window.addEventListener "beforeunload" terminate))
