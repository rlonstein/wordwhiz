(ns wordwhiz.clj.audio
  (:import (java.io File)
           (javax.sound.sampled AudioFormat
                                AudioFormat$Encoding
                                AudioInputStream
                                AudioSystem
                                Clip
                                DataLine
                                DataLine$Info
                                LineUnavailableException)
           (org.kc7bfi.jflac FLACDecoder)))


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
  (if-not (is-pcm? audio-stream) (stream-to-pcm audio-stream) audio-stream))

(defn get-audio-input-stream [fn]
  (coerce-stream-to-pcm (AudioSystem/getAudioInputStream (File. fn))))

(defn play-sound [fn]
  (let [
        stream (get-audio-input-stream fn)
        format (. stream getFormat)
        info (DataLine$Info. Clip format)
        #^Clip clip (AudioSystem/getLine info)
        ]
    (.open clip stream)
    (.start clip)))
