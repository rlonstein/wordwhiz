(ns wordwhiz.clj.test.core
  (:use [wordwhiz.clj.core])
  (:use [clojure.test]))

(def test-dict (read-dict dictfile))

(deftest test-read-dict
  (is (> (count test-dict) 1)))

(def test-tiles (tileset))

(deftest test-tileset
  (is (= 98 (count (flatten test-tiles)))))

(def test-board (fill-board test-tiles))

(deftest test-fill-board
  (is (vector? test-board)))

(deftest test-tile-at
  (are (not (nil? (tile-at test-board 0 0)))
       (not (nil? (tile-at test-board (dec (:x board-dim)) (dec (:y board-dim)))))))

(deftest test-valid-word
  (are (true? (valid-word? "AARDVARK" test-dict))
       (true? (valid-word? "ZYMURGY" test-dict))
       (true? (not (valid-word? "FRUMIOUS" test-dict)))))

(deftest test-score-word
  (is (= (score-word "AARDVARK") 128)))

(def test-game (new-game))

;; (deftest test-new-game
;;   (are (true? (:board test-game))
;;        (true? (:tiles test-game))
;;        (true? (:dictionary test-game))
;;        (nil? (:history test-game))
;;        (zero? (:score test-game))
;;        (nil? (:rack test-game))))

(deftest test-score-rack
  (is (and
       (zero? (:score (score-rack test-game)))
       (= 128 (:score (score-rack (merge test-game {:rack [\A \A \R \D \V \A \R \K]})))))))

