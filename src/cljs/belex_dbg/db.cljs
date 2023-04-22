(ns belex-dbg.db
  (:require
   [belex-dbg.config :as config]
   [belex-dbg.diri :as d]))

(def default-diri (d/->diri))

(def default-db
  {:name "belex-dbg"
   :diri default-diri
   :next-diri nil
   :files {}
   :loading-files []
   :num-visible-plats config/num-visible-plats
   :toggle-active? {}
   :active-toggles []
   :terminal (js/Terminal. #js {:convertEol true})
   :prev-state :paused
   :state :waiting
   :event-buffer #queue []
   :socket-state :ready})
