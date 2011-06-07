;;;
;;; Wordwhiz -- A letter tile game
;;;
;;; Copyright (c) R. Lonstein
;;; Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
;;;

(ns wordwhiz.clj.ui
  (:import
   (org.apache.pivot)
   (org.apache.pivot.beans BXMLSerializer)
   (org.apache.pivot.wtk Application
                         Application$UncaughtExceptionHandler
                         ApplicationContext
                         DesktopApplicationContext
                         BrowserApplicationContext
                         Frame
                         Window
                         Component))
  (:gen-class
   :implements [org.apache.pivot.wtk.Application]
   :state state
   :init init)
)

(def uidescfile (. ClassLoader getSystemResource "ui.bxml"))

(defn -init []
  [ [] (atom []) ])

(defn -main [& args]
  "Entry point for application-style (desktop) execution"
  (. DesktopApplicationContext main (. (ClassLoader/getSystemClassLoader) loadClass "wordwhiz.clj.ui") (into-array String args)))

(defn -startup [this display props]
  (. (. (org.apache.pivot.beans.BXMLSerializer.) readObject uidescfile) open display))

(defn -resume [this])

(defn -suspend [this])

(defn -shutdown [this optional]
  false)

