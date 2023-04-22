(defproject belex-debug "1.0rc8"

  :description "Belex debugger"
  :url "https://bitbucket.org/gsitech/belex-debug"

  ;; CLJS dependencies are managed by shadow-cljs (see: shadow-cljs.edn)
  ;; :dependencies []

  :min-lein-version "2.10.0"

  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :test-paths ["test/clj" "test/cljs"]
  :java-source-paths ["src/java"]
  :foreign-libs []
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :main ^:skip-aot belex_dbg.core

  :plugins [[lein-garden "0.3.0"]
            [lein-kibit "0.1.8"]
            [org.clojars.punkisdead/lein-cucumber "1.0.5"]]
  :cucumber-feature-paths ["test/clj/features"]

  :garden {:builds [{;; Optional name of the build:
                     :id "screen"
                     ;; Source paths where the stylesheet source code is
                     :source-paths ["src/styles"]
                     ;; The var containing your stylesheet:
                     :stylesheet belex_dbg.core/screen
                     ;; Compiler flags passed to `garden.core/css`:
                     :compiler {;; Where to save the file:
                                :output-to "resources/public/css/compiled/screen.css"
                                ;; Compress the output?
                                :pretty-print? true}}]}

  :clean-targets ^{:protect false}
  [:target-path "target/cljsbuild"]
  :profiles
  {:dev  [:project/dev :profiles/dev]
   :test [:project/dev :project/test :profiles/test]
   :project/dev  {}
   :project/test {}
   :profiles/dev {}
   :profiles/test {}})
