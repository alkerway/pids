(ns pids.core (:require [rum.core :as rum]
                [pids.config :as config]
                [pids.reqs :as reqs]
                [pids.poller :as poller]
                [clojure.string :as cljstr]))
(enable-console-print!)

(rum/defc progressBar < rum/reactive [percentStr]
  [:div {:style {:width "700px" :border "1px solid black"}}
  [:div {:style {:width (or (rum/react percentStr) 0)
                 :background-color "#DDD"
                 :height "20px"}} (rum/react percentStr)]])

(rum/defc linesDisp < rum/reactive [atomAsVector]
  [:div {:style {:max-height "500px" :max-width "950px" :overflow "scroll" :white-space "nowrap"}}
         (for [line (rum/react atomAsVector)]
           [:div [:span (str (:pid line) " ms:" (:ms line))]
            [:span {:style {:cursor "pointer"}
                    :on-click #(reqs/openUrl (:url line) (:unauth line))} (str " " (:url line) " ")]
            [:span (str " " (:setMRespStatus line)
                (if (:unauth line) " (unauth)") (if (:isLive line) " (live)"))]])])

(rum/defc displayContainer []
  (let [dataAtom (atom [])
        statsAtom (atom nil)]
     [:div {:style {:width "100%" :height "100%"}}
     [:button {:on-click #(poller/startPoll dataAtom statsAtom config/streamtype)} "get"]
      [:button {:on-click #(reset! dataAtom [])} "clear"]
      [:span  {:style {:font-size "11px"}} (str " token " reqs/toke)]
     (progressBar statsAtom)
     (linesDisp dataAtom)]))

(defn init [] (rum/mount (displayContainer) (.getElementById js/document "app")))
(init)
