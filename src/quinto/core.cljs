(ns quinto.core
  (:require [com.rpl.specter :refer [select ALL srange nthpath multi-path STOP]]
            [reagent.core :as r]
            [quinto.deck :refer [make-deck draw-tiles MAX-HAND-SIZE]]
            [quinto.html :refer [render-game]]
            [quinto.specter :refer [grid-values]]
            [quinto.grid :as g]))

(defonce app-state
         (r/atom {:grid g/empty-grid
                  :deck (make-deck)
                  :hand []
                  :mode {:mode/type :default}}))

(defn enter-assembling-move-mode! [selected-cell]
  (assert (contains? (set (g/find-playable-cells (@app-state :grid)))
                     selected-cell))
  (swap! app-state assoc :mode
         {:mode/type       :assembling-move
          :selected-cell   selected-cell
          :available-cells []
          :move-so-far     []}))

(defn ^:export main []
  ;(stest/instrument)

  (let [[new-deck new-hand] (draw-tiles (@app-state :deck)
                                        (@app-state :hand)
                                        MAX-HAND-SIZE)]
    (swap! app-state assoc :deck new-deck)
    (swap! app-state assoc :hand new-hand))

  (swap! app-state update-in [:grid] g/make-move [[[6 4] 7]
                                                  [[6 5] 3]
                                                  [[6 6] 5]
                                                  [[6 7] 4]
                                                  [[6 8] 1]])

  (swap! app-state update-in [:grid] g/make-move [[[5 4] 5]
                                                  [[4 4] 3]
                                                  [[3 4] 2]
                                                  [[2 4] 8]])

  (enter-assembling-move-mode! [5 5])

  (render-game app-state))

(def on-js-reload render-game)

(comment
  (@app-state :mode)
  )
