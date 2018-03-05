(ns pids.core (:require-macros [cljs.core.async.macros :refer [go go-loop]])
    (:require [rum.core :as rum]
              [pids.parser :as parser]
              [pids.reqs :as reqs]
              [clojure.string :as cljstr]
              [cljs.core.async :refer [<! timeout]]))
(enable-console-print!)

(defn randstr [length]
  (let [possible "abcdefghijklmnopqrstuvwxyz"]
    (loop [accStr ""] (if (< (count accStr) length)
      (recur (str accStr (nth possible (rand-int (count possible))))) accStr))))
                   
(defn setAll [dataAtom statsAtom filterParam]
  (let [allPids (into [] (concat (range 200000 200800) (range 202700 203000) (range 204000 205000)))
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
             isLive (if (not-empty streamUrl) (parser/isLive (:body (<! (reqs/getResponse streamUrl)))))]
         (reset! statsAtom (str (* 100 (/ (- (count allPids) (count (rest pidsArr))) (count allPids))) "%"))
       (if (not-empty sourceUrl)
         (swap! dataAtom conj
                (str currentPid " " sourceUrl "  "
                     (if (:status setMResp) (str (:status setMResp)
                         (if (= (:status unauthCheck) 403) "*" "")))
                     (if isLive (str " (Actually Live)")))))
       (if (not-empty pidsArr) (recur (rest pidsArr))
           (reset! statsAtom (str "Found " (count @dataAtom) " of " (count allPids) " pids in "
                (/ (- (.now js/performance) startTime) 1000) " seconds")))))))

(rum/defc progressBar < rum/reactive [percentStr]
  [:div {:style {:width "700px" :border "1px solid black"}}
  [:div {:style {:width (or (rum/react percentStr) 0)
                 :background-color "#DDD"
                 :height "20px"}} (rum/react percentStr)]])

(rum/defc linesDisp < rum/reactive [atomAsVector]
  [:div (for [line (rum/react atomAsVector)] [:div line])])

(rum/defc displayContainer []
  (let [dataAtom (atom [])
        statsAtom (atom nil)]
     [:div {:style {:width "100%" :height "100%"}}
     [:button {:on-click #(setAll dataAtom statsAtom "live")} "get"]
      [:button {:on-click #(reset! dataAtom [])} "clear"]
      [:span  {:style {:font-size "11px"}} (str " using token " reqs/toke)]
     (progressBar statsAtom)
     (linesDisp dataAtom)]))

(defn init [] (rum/mount (displayContainer) (.getElementById js/document "app")))
(init)
