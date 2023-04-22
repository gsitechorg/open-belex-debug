(ns belex-dbg.core-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [belex-dbg.core :as core]))

(deftest fake-test
  (testing "fake description"
    (is (= 1 2))))
