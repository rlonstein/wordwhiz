;;;
;;; Wordwhiz -- A letter tile game
;;;
;;; Copyright (c) R. Lonstein
;;; Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
;;;

(ns wordwhiz.clj.utils
  (:require (clojure.java.io)))

(defn get-system-resource [s]
  (clojure.java.io/resource s))
