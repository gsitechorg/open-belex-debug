(ns belex-debug.core-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [belex-debug.core :as core]))

(deftest fake-test
  (testing "fake description"
    (is (= 1 2))))
