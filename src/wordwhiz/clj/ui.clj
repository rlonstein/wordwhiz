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
(def debug (atom true))
(def state (ref (wordwhiz.clj.core/new-game)))

(defmacro notnull! [v]
  "convenience macro for liberally asserting not null"
  `(assert (not (nil? ~v))))

(defn debug-game-state [game]
  (when @debug
    (println "debug-game-state()" )
    (doseq [ k [:rack :board :score :board-dim :history :rack-size :playing] ]
      (println "\t" k (k game)))
    (println "\t :dictionary" (count (:dictionary game)))))

(defn attach-button-listener [btn f]
  (.. btn
      (getButtonPressListeners)
      (add (proxy [ButtonPressListener] []
             (buttonPressed [b] (f b))))))

(defn get-resource [res]
  (.. (Thread/currentThread) (getContextClassLoader) (getResource res)))

(defn get-resource-fn [res]
  (.. (get-resource res) (getFile)))

(defmulti update-widgit-image (fn [c _] (class c)))

(defmethod update-widgit-image org.apache.pivot.wtk.Button [widgit url]
  (notnull! widgit)
  (notnull! url)
  ;; (println "update-widgit-image" widgit url)
  (let [ bdata (.getButtonData widgit)
         icon  (get-resource url) ]
    (when (nil? bdata) (.setButtonData widgit (org.apache.pivot.wtk.content.ButtonData.)))
    (if (nil? icon)
      (.setButtonData widgit (org.apache.pivot.wtk.content.ButtonData.))
      (.. widgit (getButtonData) (setIcon icon)))))

(defmethod update-widgit-image org.apache.pivot.wtk.ImageView [widgit url]
  (notnull! widgit)
  (notnull! url)
  (.. widgit (setImage (get-resource url))))

;; (defn debug-iterate-ns [ns]
;;   (when @debug
;;     (println "debug-iterate-ns()")
;;     (let [i (.iterator ns)]
;;       (doseq [ k (loop [ acc [] next (.hasNext i) ]
;;                    (if next (recur (conj acc (.next i)) (.hasNext i))
;;                        acc))]
;;         (println "\t" k (.get ns k))))))

(defn get-named-component [c]
  "Return the named component or nil.
Performs getName() on org.apache.pivot.wtk.Component or stringifies the object"
  (let [name (if (instance? org.apache.pivot.wtk.Component c) (.getName c) (.toString c))
        ns (dosync (.getNamespace @serializer))
        component (.get ns name)]
    ;; (printf "get-named-component(%s):\n\tname==\"%s\"\tcomponent==%s\n" c name component)
    ;; (debug-iterate-ns ns)
    (notnull! component)
    component))

(defn get-widgit-at [x y]
  (get-named-component (str x "," y)))

(defn get-nth-rack-widgit [n]
  "Return the rack ui component at specified offset"
  (get-named-component (str "rack" "," n)))

(defn char->tileimage [c]
  (str "image/tiles/tile-" c ".png"))

(defn update-board [game & {:keys [sleepms]}]
  (doseq [col (range 0 (:x (:board-dim game)))
          row (range 0 (:y (:board-dim game)))]
    ;; notice the monkey-business here...
    ;; ui layout row,col but board structure is col,row
    (let [tile (wordwhiz.clj.core/tile-at (:board game) col row)
          tile-img (char->tileimage tile)
          widgit (get-widgit-at row col)]
      ;;      (when @debug (println "update-board():" row col tile tile-img widgit))
      (when sleepms (Thread/sleep sleepms))
      (notnull! tile-img)
      (notnull! widgit)
      (update-widgit-image widgit tile-img))))

(defn update-rack [game]
  "Update the images in the rack, run only for the ui side-effect"
  (notnull! game)
  (doseq [ idx (range 0 (:rack-size game))]
    (let [letter (wordwhiz.clj.core/rack-nth idx game)
          tile-img (char->tileimage letter)
          widgit (get-nth-rack-widgit idx)]
      ;; (println "debug: update-rack()" letter tile-img)
      (update-widgit-image widgit tile-img))))

(defn update-rackscore [game]
  (notnull! game)
  (let [score (wordwhiz.clj.core/rack->score game)
        target (get-named-component "rackscore")]
    (notnull! target)
    ;; TODO: set color of textinput "#CC3333" "#33CC33"
    (.setText target (.toString score))))

(defn update-gamescore [game]
  (notnull! game)
  (.setText (get-named-component "score") (.toString (:score game))))

(defn update-score [game]
  (update-rackscore game)
  (update-gamescore game))

(defn btn-update-board []
  ;; TODO: maybe unneeded, trying to avoid capture of global ref in btn callback
  (notnull! @state)
  (debug-game-state @state)
  (update-board @state))

(defn btn-update-rack []
  ;; TODO: maybe unneeded, trying to avoid capture of global ref in btn callback
  (notnull! @state)
  (update-rack @state))

(defn btn-update-score []
  ;; TODO: maybe unneeded, trying to avoid capture of global ref in btn callback
  (notnull! @state)
  (debug-game-state @state)
  (update-score @state))

(defn button-to-column [btn]
  "Get the column associated with the button.
relies on parsing id of widgit, returns nil on failure"
  (try
    (Integer/parseInt (nth (clojure.string/split (. btn getName) #",") 1))
    (catch NumberFormatException e)))

;; (defn startup-board [game]
;;   "Render a title sequence on the board"
;;   ;;FIXME: this doesn't work because the ui updated asynchronously
;;   (notnull! game)
;;   (let [blank [ ["space" "space" "space" "space" "space" "space" "space"]
;;                 ["space" "space" "space" "space" "space" "space" "space"]
;;                 ["space" "space" "space" "space" "space" "space" "space"]
;;                 ["space" "space" "space" "space" "space" "space" "space"]
;;                 ["space" "space" "space" "space" "space" "space" "space"]
;;                 ["space" "space" "space" "space" "space" "space" "space"]
;;                 ["space" "space" "space" "space" "space" "space" "space"]
;;                 ["space" "space" "space" "space" "space" "space" "space"]
;;                 ["space" "space" "space" "space" "space" "space" "space"]
;;                 ["space" "space" "space" "space" "space" "space" "space"]
;;                 ["space" "space" "space" "space" "space" "space" "space"]
;;                 ["space" "space" "space" "space" "space" "space" "space"] ]
;;         title [ ["space" "space" "space" "space" "space" "space" "space"]
;;                 ["space" "space" "space" "space" "T" "space" "R"]
;;                 ["space" "W" "space" "space" "I" "space" "space"]
;;                 ["space" "O" "space" "L" "L" "space" "L"]
;;                 ["space" "R" "space" "E" "E" "space" "O"]
;;                 ["space" "D" "space" "T" "space" "space" "N"]
;;                 ["space" "W" "space" "T" "G" "space" "S"]
;;                 ["space" "H" "space" "E" "A" "space" "T"]
;;                 ["space" "I" "space" "R" "M" "space" "E"]
;;                 ["space" "Z" "space" "space" "E" "space" "I"]
;;                 ["space" "space" "space" "space" "space" "space" "N"]
;;                 ["space" "space" "space" "space" "space" "space" "space"] ]
;;         ]
;;     (update-board (merge wordwhiz.clj.core/game-defaults {:board title}) :sleepms 10)
;;     (Thread/sleep 1000)
;;     (update-board (merge wordwhiz.clj.core/game-defaults {:board blank}) :sleepms 10)
;;     (update-board game)))

(defn startup-board [game]
  (update-board game))

(defn do-startup-board []
  "Invoke startup-board with the global game state"
  ;; TODO: maybe unneeded, trying to avoid capture of global ref
  (startup-board @state))

(gen-class
 :name wordwhiz.clj.ui
 :implements [org.apache.pivot.wtk.Application]
 :init init)

(defn -main [& args]
  "Entry point for application-style (desktop) execution"
  (. DesktopApplicationContext applyStylesheet uistylesheet)
  (. DesktopApplicationContext main wordwhiz.clj.ui (into-array String args))
  (Thread/sleep 1000)
  (. DesktopApplicationContext queueCallback do-startup-board true))

(defn -startup [this display props]
  "Render a title sequence on the board"
  (let [ window (. @serializer readObject uidescfile) ]
    (.open window display)))

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
                                 (alter state wordwhiz.clj.core/rack-tile (button-to-column b)))
                                (btn-update-rack)
                                (btn-update-board)
                                (btn-update-score))))

(defn reset-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (when (not @mute)
                                  (wordwhiz.clj.audio/play-sound (get-resource-fn "audio/whoosh.flac")))
                                (dosync (alter state wordwhiz.clj.core/reset-game))
                                (btn-update-board)
                                (btn-update-rack)
                                (btn-update-score))))

(defn score-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (when (not @mute)
                                  (wordwhiz.clj.audio/play-sound (get-resource-fn "audio/mechanical2.flac")))
                                (dosync (alter state wordwhiz.clj.core/score-rack))
                                (btn-update-score)
                                (btn-update-rack))))

(defn undo-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (when (not @mute)
                                  (wordwhiz.clj.audio/play-sound (get-resource-fn "audio/mechanical2.flac")))
                                (dosync (alter state wordwhiz.clj.core/undo-move))
                                (btn-update-rack)
                                (btn-update-score)
                                (btn-update-board))))

(defn newgame-attach-listener [btn]
  (attach-button-listener btn (fn [b]
                                (when (not @mute)
                                  (wordwhiz.clj.audio/play-sound (get-resource-fn "audio/toilet_flush.flac")))
                                (dosync (alter state (wordwhiz.clj.core/new-game))))))

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
