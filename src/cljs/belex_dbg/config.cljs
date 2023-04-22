(ns belex-dbg.config)

(def debug?
  ^boolean goog.DEBUG)

(goog-define ^js/String version "unknown")

(def ^:const event-buffer-capacity 128)

(def ^:const num-visible-plats (dec (* 12 5)))
