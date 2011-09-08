;;;
;;; Wordwhiz -- A letter tile game
;;;
;;; Copyright (c) R. Lonstein
;;; Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
;;;

(ns wordwhiz.clj.core
  (:require (clojure.java.io)
            [clojure.string :as str]))

(def board-dim {:x 12 :y 7})
(def rack-size 16)
(def max-word-length rack-size)

(defn get-system-resource [s]
  (. ClassLoader getSystemResource s))

(def dictfile (get-system-resource "word.list"))

;;(def debug false)

(def tile-distr
  { \E {:value 1 :frequency 12}
    \A {:value 1 :frequency 9}
    \I {:value 1 :frequency 9}
    \O {:value 1 :frequency 8}
    \N {:value 1 :frequency 6}
    \R {:value 1 :frequency 6}
    \T {:value 1 :frequency 6}
    \L {:value 1 :frequency 4}
    \S {:value 1 :frequency 4}
    \U {:value 1 :frequency 4}
    \D {:value 2 :frequency 4}
    \G {:value 2 :frequency 3}
    \B {:value 3 :frequency 2}
    \C {:value 3 :frequency 2}
    \M {:value 3 :frequency 2}
    \P {:value 3 :frequency 2}
    \F {:value 4 :frequency 2}
    \H {:value 4 :frequency 2}
    \V {:value 4 :frequency 2}
    \W {:value 4 :frequency 2}
    \Y {:value 4 :frequency 2}
    \K {:value 5 :frequency 1}
    \J {:value 8 :frequency 1}
    \X {:value 8 :frequency 1}
    \Q {:value 10 :frequency 1}
    \Z {:value 10 :frequency 1}}
  )    ;; Scrabble(tm) also (\SPACE 0 2)


(defstruct game-state :tiles :history :score :board :rack :dictionary :playing :board-dim)

(defn read-dict [fn]
  "Read a wordlist from a file, returning the newly created set"
  (with-open [reader (clojure.java.io/reader fn)]
    (set (map #(.toUpperCase %) (line-seq reader)))))

(def game-defaults {:tiles []
                    :history ()
                    :score 0
                    :board []
                    :board-dim board-dim
                    :rack []
                    :rack-size rack-size
                    :playing true
                    :dictionary (read-dict dictfile)})

(defn tileset []
  "Return a shuffled set of letters (tiles)"
  (shuffle
   (flatten
    (for [ letter (keys tile-distr) ] (replicate (:frequency (get tile-distr letter)) letter)))))

(defn fill-board [tiles]
  "Produce a game board as a vector of vectors from the supplied collection"
  (let [cols (dec (:x board-dim))]
    (loop [v [] x 0]
      (let [start (* x (:y board-dim)) end (+ start (:y board-dim))]
        (if (> x cols) v
            (recur (conj v (subvec tiles start end)) (inc x)))))))

(defn valid-word? [word dict]
  "Check word against the dictionary"
  (contains? dict (.toUpperCase word)))

(defn score-word [w]
  "Tally the values of the letters in a word based
on tile distribution and word length"
  (* (.length w) (reduce + (for [idx (range 0 (.length w))]
                             (:value (get tile-distr (.charAt w idx)))))))

(defn rack->string [game]
  "Return the game rack as a string"
  (str/join "" (:rack game)))

(defn rack->score [game]
  "Return the current score for the game rack, zero if invalid"
  (let [word (rack->string game)]
    (if (and (valid-word? word (:dictionary game))
             (>= (count word) 2))
      (score-word word)
      0)))

(defn score-rack [game]
  "Score the rack, checking validity in game dictionary, returns the new game state"
  (let [ points (rack->score game)]
    (if-not (zero? points)
      (let [ history (conj (:history game) (list \S points (:rack game) ))
             new-score (+ points (:score game)) ]
;;        (when debug (println "score-rack:" history new-score))
        (merge game {:rack [] :history history :score new-score}))
      game)))

(defn rack-full? [game]
  "Return true if the game rack is full"
  (>= (count (:rack game)) (:rack-size game)))

(defn rack-nth [i game]
  "Return letter at specified position on the rack of the supplied game"
  (get (:rack game) i))

(defn rack-tile [game col]
  "Append a tile from the given column to the rack. Returns the modified game"
  (let [tile (first (nth (:board game) col))
        column (vec (rest (nth (:board game) col)))]
;;    (when debug (println "rack-tile(): tile==" tile "col==" col))
    (if (and (not (nil? tile)) (not (rack-full? game)))
      (let [rack (conj (:rack game) tile)
            history (conj (:history game) (list \M col tile) )
            board (assoc (:board game) col column)]
        (merge game {:board board :rack rack :history history}))
      game)))

(defn tile-at [board x y]
  "Return the tile (letter) at given coordinates"
  (get (get board x) y))

(defn reset-game [game]
  "Return a game with any modified state reset to starting condition"
  (merge game {:board (fill-board (:tiles game))
               :rack (:rack game-defaults)
               :score (:score game-defaults)
               :history (:history game-defaults)}))

(defn board-col [game col]
  "Return the specified column from the board"
  (get (:board game) col))

(defn num-tiles [board]
  "Return the count of remaining tiles on the board"
  (reduce + (map count board)))

(defn open-tiles [board]
  "Return a set containing the tiles at the top row of the board"
  (remove nil? (map first board)))

(defn no-tiles-left? [game]
  "Return true if there are no tiles remaining on top row of the board"
  (zero? (count (open-tiles (:board game)))))

(defn valid-words-exist? [game]
  ;;FIXME: generate the permutations of the letters in the top row
  ;; and look them up in the dictionary, stopping at the first valid
  ;; word
  )

(defn force-resign? [game]
  "Check if the current game has a playable number of tiles left, returns boolean"
  (> 2 (num-tiles (:board game))))

(defn undo-move [game]
  "Rewind actions from the game history"
  (let [last-move (first (:history game))
        history (rest (:history game))
        action (first last-move)]
    (cond
     (nil? last-move) game
     (= \M action) (let [tile (nth last-move 2) column (nth last-move 1)]
;;                     (when debug (println "undo-move: " last-move action tile column))
                     (merge game {:history history
                                  :rack (vec (butlast (:rack game)))
                                  :board (assoc (:board game) column (vec (cons tile (board-col game column))))}))
     (= \S action) (let [points (nth last-move 1) rack (nth last-move 2)]
;;                     (when debug (println "undo-move: " last-move action points rack)) 
                     (merge game {:history history
                                  :rack rack
                                  :score (- (:score game) points)}))
     true game)))

(defn new-game []
  "Return a new populated game state"
  (let [g (merge (struct game-state) game-defaults {:tiles (tileset)})]
    (merge g {:board (fill-board (:tiles g))})))
