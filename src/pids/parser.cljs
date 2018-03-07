(ns pids.parser (:require [clojure.string :as cljstr]))

(defn constructUrl [manifestUrl text]
  (if (and manifestUrl text)
  (cond (re-matches #"^http.*" text) text
        (= (first text) "/")
          (str (aget (new js/URL manifestUrl) "origin") text)
        :else (str (cljstr/join "/"
              (pop (cljstr/split manifestUrl "/"))) "/" text)) nil))

(defn isLive [str]
  (not (re-find #"ENDLIST" str)))

(defn getStream [manifestVector]
  (first (filter #(re-matches #".+\.m3u8" %) manifestVector)))
