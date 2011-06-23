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
(def serializer (atom nil))
(def mute (atom false))
(def debug (atom false))
(def state (atom (wordwhiz.clj.core/new-game)))

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

(defn get-named-component [c]
  "Return the named component or nil.
Performs getName() on org.apache.pivot.wtk.Component or stringifies the object"
  (let [name ((if (instance? org.apache.pivot.wtk.Component c) (. c getName) (.toString c)))]
    (. (. @serializer getNamespace) get name)))

(defn get-widgit-at [x y]
  (get-named-component (str x "," y)))

(defn update-board [game]
  (doseq [row (range 0 (:y (:board-dim game)))
          col (range 0 (:x (:board-dim game)))]
    (let [widgit (get-widgit-at row col)
          tile (wordwhiz.clj.core/tile-at (:board game) row col)]
      (println "updating " widgit " at " row "/" col " with " tile)
      (update-widgit-image widgit tile))))

(defn update-rack [game]
  ;;FIXME
  )

(defn update-score [game]
  (let [score (wordwhiz.clj.core/rack->score game)
        target (get-named-component "rackscore")]
    (. target setText score)))

(defn button-to-column [btn]
  "Get the column associated with the button.
relies on parsing id of widgit, returns nil on failure"
  (try
    (Integer/parseInt (nth (clojure.string/split (. btn getName) #",") 1))
    (catch NumberFormatException e)))

(gen-class
 :name wordwhiz.clj.ui
 :implements [org.apache.pivot.wtk.Application]
 :init init)

(defn -main [& args]
  "Entry point for application-style (desktop) execution"
  (. DesktopApplicationContext applyStylesheet uistylesheet)
  (. DesktopApplicationContext main wordwhiz.clj.ui (into-array String args)))

(defn -startup [this display props]
  (reset! serializer (org.apache.pivot.beans.BXMLSerializer.))
  (. (. @serializer readObject uidescfile) open display))

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

(gen-class
 :name wordwhiz.clj.ui.Checkbox
 :extends org.apache.pivot.wtk.Checkbox
 :post-init attach-listener
 :prefix checkbox-)

(defn bbtn-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (wordwhiz.clj.audio/play-sound (get-resource-fn "audio/twig_snap.flac"))
                                (swap! state wordwhiz.clj.core/rack-tile (. b getName))
                                (update-board state))))

(defn reset-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (wordwhiz.clj.audio/play-sound (get-resource-fn "audio/whoosh.flac"))
                                (swap! state wordwhiz.clj.core/reset-game)
                                (update-board state)
                                (update-rack state)
                                (update-score state))))

(defn score-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (wordwhiz.clj.audio/play-sound (get-resource-fn "audio/mechanical2.flac"))
                                (update-score (swap! state wordwhiz.clj.core/score-rack))
                                (update-rack state))))

(defn undo-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (wordwhiz.clj.audio/play-sound (get-resource-fn "audio/mechanical2.flac"))
                                (swap! state wordwhiz.clj.core/undo-move)
                                (update-rack state)
                                (update-score state))))

(defn newgame-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (wordwhiz.clj.audio/play-sound (get-resource-fn "audio/toilet_flush.flac"))
                                (reset! state (wordwhiz.clj.core/new-game)))))

(defn quit-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (wordwhiz.clj.audio/play-sound
                                 (get-resource-fn "audio/vicki-bye.au")
                                 (fn [e] (java.lang.System/exit 0))
                                 (:stop (wordwhiz.clj.audio/listener-event-types))))))

(defn checkbox-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (let [ id (. b getName)]
                                  (cond (= id "Mute") (reset! mute (not @mute))
                                        (= id "Debug") (reset! debug (not @debug)))))))
