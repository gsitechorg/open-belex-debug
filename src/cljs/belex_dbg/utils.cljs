(ns belex-dbg.utils)

(defn zip
  ([& xss]
   (apply map vector xss)))

(defn enumerate
  ([xs]
   (map-indexed vector xs)))
