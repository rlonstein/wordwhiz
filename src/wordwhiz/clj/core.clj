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
(def dictfile (. ClassLoader getSystemResource "word.list"))

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
                    :history []
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
  (loop [v [] x 0]
    (let [start (* x (:y board-dim)) end (+ start (:y board-dim))]
      (if (> x (:x board-dim)) v
          (recur (conj v (subvec tiles start end)) (inc x))))))

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
    (if (valid-word? word (:dictionary game))
      (score-word (word))
      0)))

(defn score-rack [game]
  "Score the rack, checking validity in game dictionary, returns the new game state"
  (let [ points (rack->score game)]
    (if-not (zero? points)
      (merge game {:rack nil
                   :history (conj (:history game) \S (:score game))
                   :score (+ points (:score game))})
      game)))

(defn rack-full? [game]
  (>= (count (:rack game)) (:rack-size game)))

(defn rack-nth [i game]
  "Return letter at specified position on the rack of the supplied game"
  (get (:rack game) i))

(defn rack-tile [game col]
  "Append a tile from the given column to the rack. Returns the modified game"
  (let [tile (first (nth (:board game) col))
        column (vec (rest (nth (:board game) col)))]
    (println "tile==" tile "col==" col)
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
  (merge game {:board (fill-board (:tiles game))
               :rack (:rack game-defaults)
               :score (:score game-defaults)
               :history (:history game-defaults)}))

(defn undo-move [game]
  "Rewind actions from the game history"
  ;;FIXME
  )

(defn new-game []
  "Return a new populated game state"
  (let [g (merge (struct game-state) game-defaults {:tiles (tileset)})]
    (merge g {:board (fill-board (:tiles g))})))
