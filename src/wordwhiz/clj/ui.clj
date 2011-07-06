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
                         Checkbox
                         ButtonPressListener
                         Component
                         DesktopApplicationContext
                         Frame
                         MessageType
                         Prompt
                         PushButton
                         Window)
   (org.apache.pivot.wtk.content ButtonData)
   (org.apache.pivot.wtk.media Image
                               Picture)))

(def uidescfile (. ClassLoader getSystemResource "ui.bxml"))
(def uistylesheet "@styles.json")
(def serializer (ref (org.apache.pivot.beans.BXMLSerializer.)))
(def mute (atom false))
(def debug (atom false))
(def state (ref (wordwhiz.clj.core/new-game)))

(defn debug-game-state [game]
  (when @debug
    (println "debug-game-state()" )
    (doseq [ k [:rack :board :score :board-dim :history] ]
      (println "\t" k (k game)))))

(defn attach-button-listener [btn f]
  (.. btn
      (getButtonPressListeners)
      (add (proxy [ButtonPressListener] []
             (buttonPressed [b] (f b))))))

(defn get-resource [res]
  (.. (Thread/currentThread) (getContextClassLoader) (getResource res)))

(defn get-resource-fn [res]
  (.. (get-resource res) (getFile)))

(defn update-button-icon [b img]
  (let [ bdata (.getButtonData b)]
    (when (nil? bdata) (.setButtonData b (org.apache.pivot.wtk.content.ButtonData.)))
    (.. b (getButtonData) (setIcon img))))

(defn update-imageview [i img]
  (.. i (setImage img)))

(defn update-widgit-image [widgit name]
  (let [widgit-class (class widgit)
        url (get-resource name)]
    (when @debug (println "update-widgit-image(" widgit name ")\n\t" url))
    (cond
     (isa? widgit-class org.apache.pivot.wtk.Button) (update-button-icon widgit url)
     (isa? widgit-class org.apache.pivot.wtk.ImageView) (update-imageview widgit url))))

(defn debug-iterate-ns [ns]
  (when @debug
    (println "debug-iterate-ns()")
    (let [i (.iterator ns)]
      (doseq [ k (loop [ acc [] next (.hasNext i) ]
                   (if next (recur (conj acc (.next i)) (.hasNext i))
                       acc))]
        (println "\t" k (.get ns k))))))

(defn get-named-component [c]
  "Return the named component or nil.
Performs getName() on org.apache.pivot.wtk.Component or stringifies the object"
  (let [name (if (instance? org.apache.pivot.wtk.Component c) (.getName c) (.toString c))
        ns (dosync (.getNamespace @serializer))
        component (.get ns name)]
    (printf "get-named-component(%s):\n\tname==\"%s\"\tcomponent==%s\n" c name component)
;;    (debug-iterate-ns ns)
    (assert (not (nil? component)))
    component))

(defn get-widgit-at [x y]
  (get-named-component (str x "," y)))

(defn update-board [game]
  (doseq [col (range 0 (:x (:board-dim game)))
          row (range 0 (:y (:board-dim game)))]
    ;; notice the monkey-business here...
    ;; ui layout row,col but board structure is col,row
    (let [tile (wordwhiz.clj.core/tile-at (:board game) col row)
          tile-img (str "image/tiles/tile-" tile ".png" )
          widgit (get-widgit-at row col)]
;;      (when @debug (println "update-board():" row col tile tile-img widgit))
      (update-widgit-image widgit tile-img))))

(defn update-rack [game]
  ;;FIXME
  )

(defn update-score [game]
  (let [score (wordwhiz.clj.core/rack->score game)
        target (get-named-component "rackscore")]
    (assert (not (nil? target)))
    (.setText target (str score))))

(defn button-to-column [btn]
  "Get the column associated with the button.
relies on parsing id of widgit, returns nil on failure"
  (try
    (Integer/parseInt (nth (clojure.string/split (. btn getName) #",") 1))
    (catch NumberFormatException e)))

(defn startup-board [game]
  (let [blank [ ["space" "space" "space" "space" "space" "space" "space"]
                ["space" "space" "space" "space" "space" "space" "space"]
                ["space" "space" "space" "space" "space" "space" "space"]
                ["space" "space" "space" "space" "space" "space" "space"]
                ["space" "space" "space" "space" "space" "space" "space"]
                ["space" "space" "space" "space" "space" "space" "space"]
                ["space" "space" "space" "space" "space" "space" "space"]
                ["space" "space" "space" "space" "space" "space" "space"]
                ["space" "space" "space" "space" "space" "space" "space"]
                ["space" "space" "space" "space" "space" "space" "space"]
                ["space" "space" "space" "space" "space" "space" "space"]
                ["space" "space" "space" "space" "space" "space" "space"] ]
        title [ ["space" "space" "space" "space" "space" "space" "space"]
                ["space" "space" "space" "space" "T" "space" "R"]
                ["space" "W" "space" "space" "I" "space" "space"]
                ["space" "O" "space" "L" "L" "space" "L"]
                ["space" "R" "space" "E" "E" "space" "O"]
                ["space" "D" "space" "T" "space" "space" "N"]
                ["space" "W" "space" "T" "G" "space" "S"]
                ["space" "H" "space" "E" "A" "space" "T"]
                ["space" "I" "space" "R" "M" "space" "E"]
                ["space" "Z" "space" "space" "E" "space" "I"]
                ["space" "space" "space" "space" "space" "space" "N"]
                ["space" "space" "space" "space" "space" "space" "space"] ]
        ]
    (update-board (merge wordwhiz.clj.core/game-defaults {:board title}))
    (update-board (merge wordwhiz.clj.core/game-defaults {:board blank}))
    (update-board game)))

(gen-class
 :name wordwhiz.clj.ui
 :implements [org.apache.pivot.wtk.Application]
 :init init)

(defn -main [& args]
  "Entry point for application-style (desktop) execution"
  (. DesktopApplicationContext applyStylesheet uistylesheet)
  (. DesktopApplicationContext main wordwhiz.clj.ui (into-array String args)))

(defn -startup [this display props]
  (let [ window (. @serializer readObject uidescfile) ]
    (.open window display)
    (startup-board @state)))

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
                                (when (not @mute)
                                  (wordwhiz.clj.audio/play-sound (get-resource-fn "audio/twig_snap.flac")))
                                (dosync
                                 (alter state wordwhiz.clj.core/rack-tile (.getName b))
                                 (debug-game-state @state)
                                 (update-board @state)))))

(defn reset-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (when (not @mute)
                                  (wordwhiz.clj.audio/play-sound (get-resource-fn "audio/whoosh.flac")))
                                (dosync (alter state wordwhiz.clj.core/reset-game))
                                (debug-game-state @state)
                                (update-board @state)
                                (update-rack @state)
                                (update-score @state))))

(defn score-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (when (not @mute)
                                  (wordwhiz.clj.audio/play-sound (get-resource-fn "audio/mechanical2.flac")))
                                (dosync
                                 (debug-game-state @state)
                                 (update-score (alter state wordwhiz.clj.core/score-rack))
                                 (update-rack @state)))))

(defn undo-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (when (not @mute)
                                  (wordwhiz.clj.audio/play-sound (get-resource-fn "audio/mechanical2.flac")))
                                (dosync
                                 (debug-game-state state)
                                 (alter state wordwhiz.clj.core/undo-move)
                                 (update-rack @state)
                                 (update-score @state)))))

(defn newgame-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (when (not @mute)
                                  (wordwhiz.clj.audio/play-sound (get-resource-fn "audio/toilet_flush.flac")))
                                (dosync
                                 (debug-game-state @state)
                                 (alter state (wordwhiz.clj.core/new-game))
                                 (debug-game-state @state)))))

(defn quit-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (if (not @mute)
                                  (wordwhiz.clj.audio/play-sound
                                   (get-resource-fn "audio/vicki-bye.au")
                                   (fn [e] (java.lang.System/exit 0))
                                   (:stop (wordwhiz.clj.audio/listener-event-types)))
                                  (java.lang.System/exit 0)))))

(defn checkbox-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (let [ id (. b getName)]
                                  (cond (= id "btnMute") (reset! mute (not @mute))
                                        (= id "btnDebug") (reset! debug (not @debug)))))))
