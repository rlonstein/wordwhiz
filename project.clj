(defproject wordwhiz.clj "0.0.1-SNAPSHOT"
  :description "Wordwhiz - A letter tile game"
  :license {:name "MIT License"
            :url "http://www.opensource.org/licenses/mit-license.php"}
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.apache.pivot/pivot "2.0"]]
  :jar-name "wordwhiz.jar"
  :warn-on-reflection true
  :main wordwhiz.clj.core
  :aot [wordwhiz.clj.core])
