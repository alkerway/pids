(ns pids.poller (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require     [pids.config :as config]
                [pids.parser :as parser]
                [pids.reqs :as reqs]
                [clojure.string :as cljstr]
                [cljs.core.async :refer [<!]]))

(defn startPoll [dataAtom statsAtom filterParam]
  (let [allPids config/pidrange
        startTime (.now js/performance)]
   (go-loop [pidsArr allPids]
       (let [currentPid (first pidsArr)
             status (:status (<! (reqs/getNbcJson currentPid "event_status_")))
             sourceUrl (if (= filterParam status)
               (:sourceUrl (first (:videoSources (<! (reqs/getNbcJson currentPid "live_sources_"))))) nil)
             unauthCheck (if sourceUrl (<! (reqs/getResponse sourceUrl)))
             setMResp (if (= (:status unauthCheck) 403) (<! (reqs/getResponse sourceUrl true)) unauthCheck)
             streamUrl (if (= (:status setMResp) 200)
                         (parser/constructUrl sourceUrl (parser/getStream (cljstr/split-lines (:body setMResp)))))
             streamResp (if (not-empty streamUrl) (<! (reqs/getResponse streamUrl)))
             isLive (if (= (:status streamResp) 200) (parser/isLive (:body streamResp)))
             discoCount (if (and config/showdisco (= (:status streamResp) 200)) (parser/discoCount (:body streamResp)))
             mediaSequence (if (and config/showms (= (:status streamResp) 200)) (parser/mSequence (:body streamResp)))]
         (reset! statsAtom (str (* 100 (/ (- (count allPids) (count (rest pidsArr))) (count allPids))) "%"))
       (if (not-empty sourceUrl)
         (swap! dataAtom conj
                {:pid currentPid :url sourceUrl :setMRespStatus (:status setMResp)
                 :unauth (= (:status unauthCheck) 200) :isLive isLive :ms mediaSequence :dc discoCount}))
       (if (not-empty pidsArr) (recur (rest pidsArr))
           (reset! statsAtom (str "Found " (count @dataAtom) " of " (count allPids) " pids in "
                (/ (- (.now js/performance) startTime) 1000) " seconds")))))))
