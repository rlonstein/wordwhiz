(defproject wordwhiz.clj "0.2.0-SNAPSHOT"
  :description "Wordwhiz - A letter tile game"
  :license {:name "MIT License"
            :url "http://www.opensource.org/licenses/mit-license.php"}
  :dependencies [
		 [org.clojure/clojure-contrib "1.2.0"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/java.jdbc "0.7.3"]
                 [org.xerial/sqlite-jdbc "3.20.1"]
                 [com.github.trilarion/vorbis-support "1.1.0"]
                 [org.apache.pivot/pivot-core "2.0.5"]
                 [org.apache.pivot/pivot-wtk "2.0.5"]
                 [org.apache.pivot/pivot-wtk-terra "2.0.5"]]
  :dev-dependencies [[clojure-source "1.8.0"]]
  :resources-path "resources"
  :jar-name "wordwhiz.jar"
  :warn-on-reflection false
  :main wordwhiz.clj.ui
  :aot [wordwhiz.clj.ui])
