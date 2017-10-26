;;;
;;; Wordwhiz -- A letter tile game
;;;
;;; Copyright (c) R. Lonstein
;;; Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
;;;

(ns wordwhiz.clj.audio
  (:require
   [clojure.java.io :as io]
   [wordwhiz.clj.utils :as utils])
  (:import
   (java.io ByteArrayOutputStream
            ByteArrayInputStream)
   (javax.sound.sampled AudioFormat
                                AudioFormat$Encoding
                                AudioInputStream
                                AudioSystem
                                Clip
                                DataLine
                                DataLine$Info
                                LineEvent
                                LineEvent$Type
                                LineListener
                                LineUnavailableException)))

(def mute (atom false))
(def sounds (atom {}))

(defn toggle-mute []
  (reset! mute (not @mute)))

(defn is-pcm? [audio-stream]
  (= (.. audio-stream (getFormat) (getEncoding)) AudioFormat$Encoding/PCM_SIGNED))

(defn stream-to-pcm [audio-stream]
  "convert audio input stream to a pcm stream"
  (let [ audio-format (. audio-stream getFormat)
        new-format (AudioFormat. AudioFormat$Encoding/PCM_SIGNED
                                 (. audio-format getSampleRate)
                                 16
                                 (. audio-format getChannels)
                                 (* 2 (. audio-format getChannels))
                                 (. audio-format getSampleRate)
                                 false) ]
    (AudioSystem/getAudioInputStream new-format audio-stream)))

(defn coerce-stream-to-pcm [audio-stream]
  (if-not (is-pcm? audio-stream)
    (stream-to-pcm audio-stream)
    audio-stream))

(defn get-audio-input-stream [url]
  (coerce-stream-to-pcm (AudioSystem/getAudioInputStream url)))

(defn get-bytes [url]
  (with-open [src (io/input-stream url)
              dst (ByteArrayOutputStream.)]
    (io/copy src dst)
    (.toByteArray dst)))

(defn listener-event-types []
  "Return mapping of event names to underlying implementation types"
  {
   :start (LineEvent$Type/START)
   :stop  (LineEvent$Type/STOP)
   :open  (LineEvent$Type/OPEN)
   :close (LineEvent$Type/CLOSE)
   }
  )

(defn play [bytes & [listener-fn event-type]]
  (if-not @mute
    (let [stream (get-audio-input-stream (ByteArrayInputStream. bytes))
          format (. stream getFormat)
          info (DataLine$Info. Clip format)
          #^Clip clip (AudioSystem/getLine info)]
      (if-not (nil? listener-fn)
        (. clip addLineListener (proxy [LineListener] []
                                  (update [event]
                                    (if (= (. event getType) event-type)
                                      (listener-fn event))))))
      (.open clip stream)
      (.start clip)
      (.drain clip)
      (.stop clip)
      (.close clip)
      (.close stream)))
  nil)

(defn preload []
  (let [ clips {:cheer "audio/cheer.ogg"
                :tile "audio/wooden_ball.ogg"
                :reset "audio/whoosh.ogg"
                :clunk "audio/mechanical2.ogg"
                :flush "audio/toilet_flush.ogg"
                :bye "audio/vicki-bye.ogg"} ]
    (reset! sounds
            (zipmap (keys clips)
                    (map #(get-bytes (utils/get-system-resource %1)) (vals clips))))))
