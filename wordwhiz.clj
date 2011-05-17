;;;
;;; Wordwhiz -- A word puzzle game
;;;
;;; utils.clj
;;;
;;; Copyright (c) R. Lonstein
;;; Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
;;;

(ns com.lonsteins.wordwhiz
  (:require (clojure.contrib))
  (:import (java.util HashSet)
           (javax.swing JFrame JPanel JButton JTextField JLabel SwingUtilities))
  (:use (clojure.contrib miglayout swing-utils)))

(def board-dim '(10 8))
(def rack-size 16)
(def max-word-length rack-size)
(def dictfile
  ;;FIXME
  )

(def tile-data
  ((\E 1 12)
   (\A 1 9)
   (\I 1 9)
   (\O 1 8)
   (\N 1 6)
   (\R 1 6)
   (\T 1 6)
   (\L 1 4)
   (\S 1 4)
   (\U 1 4)
   (\D 2 4)
   (\G 2 3)
   (\B 3 2)
   (\C 3 2)
   (\M 3 2)
   (\P 3 2)
   (\F 4 2)
   (\H 4 2)
   (\V 4 2)
   (\W 4 2)
   (\Y 4 2)
   (\K 5 1)
   (\J 8 1)
   (\X 8 1)
   (\Q 10 1)
   (\Z 10 1)))    ;; Scrabble(tm) also (\SPACE 0 2)

(defn read-dict [fn]
  "Read a wordlist from a file, returning the newly created set"
  (let [dict (HashSet. 100000)]
    (with-open [reader (java.io.BufferedReader.
                        (java.io.FileReader. fn))]
      (doseq [ln (line-seq reader)] (.add dict ln)))
    dict))
