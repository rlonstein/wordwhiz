;;;
;;; Wordwhiz -- A word puzzle game
;;;
;;; utils.clj
;;;
;;; Copyright (c) R. Lonstein
;;; Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
;;;

(ns com.lonsteins.wordwhiz
  (:require (clojure.contrib)
            (clojure.contrib.seq-utils)
            [clojure.string :as str])
  (:import (java.util HashSet)
           (javax.swing JFrame JPanel JButton JTextField JLabel SwingUtilities)
           (net.miginfocom.layout)
           (net.miginfocom.swing))
  (:use (clojure.contrib miglayout swing-utils)))

(def board-dim {:x 12 :y 7})
(def rack-size 16)
(def max-word-length rack-size)
(def dictfile "/Users/lonstein/git/wordwhiz.clj/word.list"
  ;;FIXME: pick this up at load
  )

(defstruct tile-data :value :frequency)
(def tile-distr
  { \E (struct-map tile-data :value 1 :frequency 12)
    \A (struct-map tile-data :value 1 :frequency 9)
    \I (struct-map tile-data :value 1 :frequency 9)
    \O (struct-map tile-data :value 1 :frequency 8)
    \N (struct-map tile-data :value 1 :frequency 6)
    \R (struct-map tile-data :value 1 :frequency 6)
    \T (struct-map tile-data :value 1 :frequency 6)
    \L (struct-map tile-data :value 1 :frequency 4)
    \S (struct-map tile-data :value 1 :frequency 4)
    \U (struct-map tile-data :value 1 :frequency 4)
    \D (struct-map tile-data :value 2 :frequency 4)
    \G (struct-map tile-data :value 2 :frequency 3)
    \B (struct-map tile-data :value 3 :frequency 2)
    \C (struct-map tile-data :value 3 :frequency 2)
    \M (struct-map tile-data :value 3 :frequency 2)
    \P (struct-map tile-data :value 3 :frequency 2)
    \F (struct-map tile-data :value 4 :frequency 2)
    \H (struct-map tile-data :value 4 :frequency 2)
    \V (struct-map tile-data :value 4 :frequency 2)
    \W (struct-map tile-data :value 4 :frequency 2)
    \Y (struct-map tile-data :value 4 :frequency 2)
    \K (struct-map tile-data :value 5 :frequency 1)
    \J (struct-map tile-data :value 8 :frequency 1)
    \X (struct-map tile-data :value 8 :frequency 1)
    \Q (struct-map tile-data :value 10 :frequency 1)
    \Z (struct-map tile-data :value 10 :frequency 1) }
  )    ;; Scrabble(tm) also (\SPACE 0 2)


(defstruct game-state :tiles :history :score :board :rack :dictionary)

(defn read-dict [fn]
  "Read a wordlist from a file, returning the newly created set"
  (let [dict (HashSet. 100000)]
    (with-open [reader (java.io.BufferedReader.
                        (java.io.FileReader. fn))]
      (doseq [ln (line-seq reader)] (.add dict (.toUpperCase ln))))
    dict))

(defn tileset []
  "Return a shuffled set of letters (tiles)"
  (shuffle
   (flatten
    (for [ letter (keys tile-distr) ] (replicate (:frequency (get tile-distr letter)) letter)))))

(defn fill-board [tiles]
  "Produce a game board as a vector of vectors from the supplied collection"
  (for [x (range 0 (:x board-dim))]
    (let [ start (* (:y board-dim) x) end (+ start (:y board-dim))]
      (subvec tiles start end))))

(defn valid-word? [word dict]
  "Check word against the dictionary"
  ;; this is just to hide the implementation...
  (.contains dict (.toUpper word)))

(defn score-word [w]
  "Tally the values of the letters in a word based
on tile distribution and word length"
  (* (.length w) (reduce + (for [idx (range 0 (.length w))]
                             (:value (get tile-distr (.charAt w idx)))))))

(defn rack-to-string [game]
  "Return the rack (list) as a string"
  (str/join "" (:rack game)))

(defn score-rack [game]
  "Score the rack, updating game state, checking validity in game dictionary"
  ;;FIXME
  )

(defn rack-full? [game]
  (if (>= (:rack game) rack-size) true false))

(defn rack-tile [col game]
  "Append a tile from the given column to the rack. Returns the tile on success, nil on failure"
      ;;FIXME
      ))

(defn ui-rack-tile [col game]
  ;;FIXME
  )

(defn undo-move [game]
  "Rewind actions from the game history"
  ;;FIXME
  )

(defn ui-undo-move [game]
  ;;FIXME
  )

(def game-defaults {:tiles nil
                    :history nil
                    :score 0
                    :board nil
                    :rack nil
                    :dictionary (read-dict dictfile)})

(defn new-game []
  "Return a new populated game state"
  (let [g (merge (struct game-state) game-defaults {:tiles (tileset)})]
    (merge g {:board (fill-board (:tiles g))})))
