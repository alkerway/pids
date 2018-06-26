(ns pids.reqs (:require-macros [cljs.core.async.macros :refer [go]])
    (:require
   [pids.config :as config]
   [cljs-http.client :as http]
   [cljs.core.async :refer [<! timeout]]))

(def toke "?token=exp=1675719581~acl=/hls/live/*~hmac=af5befe22fb8b4d6731824316370d384546418b52d902591b53447d6b7dde685")

(defn getResponse
  ([url] (getResponse url false))
  ([url isAuth]
  (go (let [response (<! (http/get (str url (if isAuth toke "")) {:with-credentials? isAuth}))]
        {:body (:body response) :status (:status response)}))))

(defn getNbcJson [pid type]
  (go (:body (<! (getResponse (str "http://stream.nbcsports.com/data/" type pid ".json"))))))

(defn openUrl [url unauth]
  (let [encodedUrl (.encodeURIComponent js/window (str url (if (not unauth) toke)))]
    (.open js/window (str config/mvBase "url=" encodedUrl "&showVideo=1" (if (not unauth) "&xhrCredentials=1")) "_blank")))
