(defproject wordwhiz.clj "0.1.0-SNAPSHOT"
  :description "Wordwhiz - A letter tile game"
  :license {:name "MIT License"
            :url "http://www.opensource.org/licenses/mit-license.php"}
  :dependencies [[org.clojure/clojure "1.2.1"]
		 [org.clojure/clojure-contrib "1.2.0"]
                 [org.apache.pivot/pivot-core "2.0.1"]
                 [org.apache.pivot/pivot-wtk "2.0.1"]
                 [org.apache.pivot/pivot-wtk-terra "2.0.1"]
                 [jflac/jflac-codec "1.4.0-SNAPSHOT"]]
  :dev-dependencies [[clojure-source "1.2.1"]]
  :resources-path "resources"
;  :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n"]
  :jar-name "wordwhiz.jar"
  :warn-on-reflection false
  :main wordwhiz.clj.ui
  :aot [wordwhiz.clj.ui])
