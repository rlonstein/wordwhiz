;;;
;;; Wordwhiz -- A letter tile game
;;;
;;; Copyright (c) R. Lonstein
;;; Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
;;;

(ns wordwhiz.clj.ui
  (:import
   (java.net URL)
   (org.apache.pivot)
   (org.apache.pivot.beans BXMLSerializer
                           Bindable)
   (org.apache.pivot.util Resources
                          ListenerList)
   (org.apache.pivot.collections Map)
   (org.apache.pivot.wtk Alert
                         Application
                         Application$UncaughtExceptionHandler
                         ApplicationContext
                         BrowserApplicationContext
                         Button
                         ButtonPressListener
                         Component
                         DesktopApplicationContext
                         Frame
                         MessageType
                         PushButton
                         Window)))

(def uidescfile (. ClassLoader getSystemResource "ui.bxml"))
(def uistylesheet "@styles.json")


(defn attach-button-listener [btn f]
  (.. btn
      (getButtonPressListeners)
      (add (proxy [ButtonPressListener] []
             (buttonPressed [b] (f b))))))

(gen-class
 :name wordwhiz.clj.ui
 :implements [org.apache.pivot.wtk.Application]
 :state state
 :init init)

(defn -main [& args]
  "Entry point for application-style (desktop) execution"
  (. DesktopApplicationContext applyStylesheet uistylesheet)
  (. DesktopApplicationContext main (. (ClassLoader/getSystemClassLoader) loadClass "wordwhiz.clj.ui") (into-array String args)))

(defn -startup [this display props]
   (. (. (org.apache.pivot.beans.BXMLSerializer.) readObject uidescfile) open display))

(defn -resume [this])

(defn -suspend [this])

(defn -shutdown [this optional]
   false)

(defn -init []
  [ [] (atom []) ])

(gen-class
 :name wordwhiz.clj.ui.BoardButton
 :extends org.apache.pivot.wtk.PushButton
 :post-init attach-listener
 :prefix bbtn-)

(gen-class
 :name wordwhiz.clj.ui.ScoreButton
 :extends org.apache.pivot.wtk.PushButton
 :post-init attach-listener
 :prefix score-)

(gen-class
 :name wordwhiz.clj.ui.UndoButton
 :extends org.apache.pivot.wtk.PushButton
 :post-init attach-listener
 :prefix undo-)

(gen-class
 :name wordwhiz.clj.ui.ResetButton
 :extends org.apache.pivot.wtk.PushButton
 :post-init attach-listener
 :prefix reset-)

(gen-class
 :name wordwhiz.clj.ui.NewGameButton
 :extends org.apache.pivot.wtk.PushButton
 :post-init attach-listener
 :prefix newgame-)

(gen-class
 :name wordwhiz.clj.ui.QuitButton
 :extends org.apache.pivot.wtk.PushButton
 :post-init attach-listener
 :prefix quit-)

(defn bbtn-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (println "I'm board button", (. b getName)))))

(defn reset-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (println "reset button"))))

(defn score-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (println "score button"))))

(defn undo-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (println "undo button"))))

(defn newgame-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (println "newgame button"))))

(defn quit-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (println "quit button"))))

