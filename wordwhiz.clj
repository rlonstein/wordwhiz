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
            (clojure.contrib.seq-utils))
  (:import (java.util HashSet)
           (javax.swing JFrame JPanel JButton JTextField JLabel SwingUtilities))
  (:use (clojure.contrib miglayout swing-utils)))

(def board-dim '(10 8))
(def rack-size 16)
(def max-word-length rack-size)
(def dictfile
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
    (for [ letter (keys tile-dist) ] (replicate (:frequency (get tile-dist letter)) letter)))))


(defn valid-word? [word dict]
  "Check word against the dictionary"
  ;; this is just to hide the implementation...
  (.contains dict (.toUpper word)))
