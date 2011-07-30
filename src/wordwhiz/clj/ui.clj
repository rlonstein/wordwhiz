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
                         Sheet
                         Window)
   (org.apache.pivot.wtk.content ButtonData)
   (org.apache.pivot.wtk.media Image
                               Picture)))

(def uifiles {:main "ui.bxml"
              :rules "intro.bxml"
              :win "win.bxml"
              :lose "game-over.bxml"})
(def uistylesheet "@styles.json")
(def serializer (ref (org.apache.pivot.beans.BXMLSerializer.)))
(def window (ref nil))
(def mute (atom false))
(def debug (atom false))
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

;; (defn get-resource-fn [res]
;;   (.. (get-resource res) (getFile)))

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
  (str "image/tiles/tile-ivory-" c ".png"))

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

(defn get-score-style [game]
  "Return a pivot style for the score ui element based on game state"
  (let [color {:normal "#000000" :bad "#CC3333" :good "#33CC33"}]
    (format "{color: '%S'}" (cond (nil? (first (:rack game))) (:normal color)
                                  (zero? (wordwhiz.clj.core/rack->score game)) (:bad color)
                                  true (:good color)))))

(defn update-rackscore [game]
  "Modify the rack score ui elements"
  (notnull! game)
  (let [target (get-named-component "rackscore")]
    (notnull! target)
    (.setStyles target (get-score-style game))
    (.setText target (.toString (wordwhiz.clj.core/rack->score game)))))

(defn update-gamescore [game]
  "Modify the game score ui element"
  (notnull! game)
  (.setText (get-named-component "score") (.toString (:score game))))

(defn update-score [game]
  "Modify the ui elements for scores"
  (update-rackscore game)
  (update-gamescore game))

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

(defn set-btn-enabled [name enabled]
  (.setEnabled (get-named-component name) enabled))

(defn toggle-score-btn [game]
  (set-btn-enabled "btnScore" (not (zero? (wordwhiz.clj.core/rack->score game)))))

(defn toggle-undo-btn [game]
  (set-btn-enabled "btnUndo" (not (nil? (first (:history game))))))

(defn startup-board [game]
  (update-board game)
  (toggle-score-btn game)
  (toggle-undo-btn game))


(defn do-startup-board []
  "Invoke startup-board with the global game state"
  ;; TODO: maybe unneeded, trying to avoid capture of global ref
  (startup-board @state))


(gen-class
 :name wordwhiz.clj.ui
 :implements [org.apache.pivot.wtk.Application]
 :main true)

(defn -main [& args]
  "Entry point for application-style (desktop) execution"
  (. DesktopApplicationContext applyStylesheet uistylesheet)
  (. DesktopApplicationContext main wordwhiz.clj.ui (into-array String args))
  (. DesktopApplicationContext queueCallback do-startup-board true))

(defn -startup [this display props]
  "Render the ui"
  (dosync
   (ref-set window
            (.readObject @serializer (wordwhiz.clj.core/get-system-resource (:main uifiles)))))
  (.setPreferredSize @window 800 468)
  (.open @window display)
  (.. (org.apache.pivot.beans.BXMLSerializer.)
      (readObject (wordwhiz.clj.core/get-system-resource (:rules uifiles)))
      (open display @window)))

(defn -resume [this])

(defn -suspend [this])

(defn -shutdown [this optional]
   false)

;; (defn -init []
;; [ [] (atom []) ])

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
  "post-init for 'BoardButton', attaches action func"
  (attach-button-listener btn (fn [b]
                                (when (not @mute)
                                  (wordwhiz.clj.audio/play-sound (get-resource "audio/twig_snap.flac")))
                                (dosync
                                 (alter state wordwhiz.clj.core/rack-tile (button-to-column b)))
                                (doto @state
                                  (toggle-score-btn)
                                  (toggle-undo-btn)
                                  (update-rack)
                                  (update-board)
                                  (update-score)))))

(defn reset-attach-listener [btn]
  "post-init for 'ResetButton', attaches action func"
  (attach-button-listener btn (fn [b]
                                (when (not @mute)
                                  (wordwhiz.clj.audio/play-sound (get-resource "audio/whoosh.flac")))
                                (dosync (alter state wordwhiz.clj.core/reset-game))
                                (doto @state
                                  (update-board)
                                  (update-rack)
                                  (update-score)
                                  (toggle-score-btn)
                                  (toggle-undo-btn)))))

(defn score-attach-listener [btn]
  "post-init for 'ScoreButton', attaches action func"
  (attach-button-listener btn (fn [b]
                                (when (not (zero? (wordwhiz.clj.core/rack->score @state)))
                                  (when (not @mute)
                                    (wordwhiz.clj.audio/play-sound (get-resource "audio/mechanical2.flac")))
                                  (dosync (alter state wordwhiz.clj.core/score-rack))
                                  (doto @state
                                    (toggle-score-btn)
                                    (update-score)
                                    (update-rack))))))

(defn undo-attach-listener [btn]
  "post-init for 'UndoButton', attaches action func"  
  (attach-button-listener btn (fn [b]
                                (when (not (zero? (count (:history @state))))
                                  (when (not @mute)
                                    (wordwhiz.clj.audio/play-sound (get-resource "audio/mechanical2.flac")))
                                  (dosync (alter state wordwhiz.clj.core/undo-move))
                                  (doto @state
                                    (toggle-score-btn)
                                    (toggle-undo-btn)
                                    (update-rack)
                                    (update-score)
                                    (update-board))))))

(defn newgame-attach-listener [btn]
  "post-init for 'NewGameButton', attaches action func"
  (attach-button-listener btn (fn [b]
                                (when (not @mute)
                                  (wordwhiz.clj.audio/play-sound (get-resource "audio/toilet_flush.flac")))
                                (dosync (ref-set state (wordwhiz.clj.core/new-game)))
                                (doto @state
                                  (toggle-score-btn)
                                  (update-rack)
                                  (update-score)
                                  (update-board)))))

(defn quit-attach-listener [btn]
  "post-init for 'QuitButton', attaches action func"
  (attach-button-listener btn (fn [b]
                                (if (not @mute)
                                  (wordwhiz.clj.audio/play-sound (get-resource "audio/vicki-bye.au")
                                   (fn [e] (java.lang.System/exit 0))
                                   (:stop (wordwhiz.clj.audio/listener-event-types)))
                                  (java.lang.System/exit 0)))))

(defn checkbox-attach-listener [btn]
  "post-init for 'CheckBox', attaches action func"
  (attach-button-listener btn (fn [b]
                                (let [ id (. b getName)]
                                  (cond (= id "btnMute") (reset! mute (not @mute))
                                        (= id "btnDebug") (reset! debug (not @debug)))))))
