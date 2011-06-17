;;;
;;; Wordwhiz -- A letter tile game
;;;
;;; Copyright (c) R. Lonstein
;;; Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
;;;

(ns wordwhiz.clj.ui
  (:require wordwhiz.clj.core
            wordwhiz.clj.audio)
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
                         Prompt
                         PushButton
                         Window)
   (org.apache.pivot.wtk.media Image)))

(def uidescfile (. ClassLoader getSystemResource "ui.bxml"))
(def uistylesheet "@styles.json")
(def state (ref {:game {}}))

(defn attach-button-listener [btn f]
  (.. btn
      (getButtonPressListeners)
      (add (proxy [ButtonPressListener] []
             (buttonPressed [b] (f b))))))

(defn get-resource [res]
  (.. (Thread/currentThread) (getContextClassLoader) (getResource res)))

(defn get-resource-fn [res]
  (.. (get-resource res) (getFile)))

(defn get-image [res]
  (. (Image.) load (get-resource res)))

(defn update-widgit-image [widgit name]
  (let [ widgit-class (class widgit) ]
    (cond (isa? widgit-class org.apache.pivot.wtk.Button) (.. widgit (setIcon) (get-image name))
          (isa? widgit-class org.apache.pivot.wtk.ImageView) (.. widgit (setImage) (get-image name)))))

(defn get-widgit-at [container x y]
  (. container getNamedComponent (str x "," y)))

(defn update-board [game container]
  (doseq [row (range 0 (:y (:board-dim game)))
          col (range 0 (:x (:board-dim game)))]
    (update-widgit-image (get-widgit-at row col) (wordwhiz.clj.core/tile-at (:board game) row col))))


(gen-class
 :name wordwhiz.clj.ui
 :implements [org.apache.pivot.wtk.Application]
 :init init)

(defn -main [& args]
  "Entry point for application-style (desktop) execution"
  (. DesktopApplicationContext applyStylesheet uistylesheet)
  (. DesktopApplicationContext main wordwhiz.clj.ui (into-array String args)))

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
                                (wordwhiz.clj.audio/play-sound (get-resource-fn "audio/twig_snap.flac"))
                                (println "I'm board button", (. b getName)))))

(defn reset-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (wordwhiz.clj.audio/play-sound (get-resource-fn "audio/whoosh.flac"))
                                (println "reset button"))))

(defn score-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (wordwhiz.clj.audio/play-sound (get-resource-fn "audio/mechanical2.flac"))
                                (println "score button"))))

(defn undo-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (wordwhiz.clj.audio/play-sound (get-resource-fn "audio/mechanical2.flac"))
                                (println "undo button"))))

(defn newgame-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (wordwhiz.clj.audio/play-sound (get-resource-fn "audio/toilet_flush.flac"))
                                (println "newgame button"))))

(defn quit-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (wordwhiz.clj.audio/play-sound
                                 (get-resource-fn "audio/vicki-bye.au")
                                 (fn [e] (java.lang.System/exit 0))))))

