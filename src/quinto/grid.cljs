(ns quinto.grid
  (:require [clojure.spec.alpha :as s]
            [quinto.specs :as sp :refer [MAX-RUN-LENGTH GRID-HEIGHT GRID-WIDTH]]))

(def empty-grid (vec (repeat GRID-WIDTH (vec (repeat GRID-HEIGHT nil)))))

(defn make-move
  "Applies `move` to `grid`."
  [grid move]
  (reduce (fn [grid [[x y] value]]
            (assert (nil? (get-in grid [x y])))
            (assoc-in grid [x y] value))
          grid
          move))

(s/fdef make-move
  :args (s/cat :grid ::sp/grid :move ::sp/move)
  :ret ::sp/grid)

(defn find-empty-cells [grid]
  ; TODO is there some cool way to use specter for this?
  (apply concat
         (for [x (range (count grid))]
           (for [y (range (count (grid x)))
                 :when (nil? (get-in grid [x y]))]
             [x y]))))

(s/fdef find-empty-cells
  :args (s/cat :grid ::sp/grid)
  :ret (s/coll-of ::sp/cell))

(defn cell-is-on-grid [grid x y]
  (and (>= x 0)
       (< x (count grid))
       (>= y 0)
       (< y (count (first grid)))))

(s/fdef cell-is-on-grid
  :args (s/cat :grid ::sp/grid :cell (s/cat :x int? :y int?))
  :ret boolean?)

(defn find-runs
  [grid x y]
  (let [run-in-direction (fn [x-direction y-direction]
                           (reduce (fn [[run-length run-sum] num-steps-in-direction]
                                     ; Find the position of the cell we're currently examining.
                                     (let [run-x (+ x (* x-direction num-steps-in-direction))
                                           run-y (+ y (* y-direction num-steps-in-direction))]
                                       (if (or (not (cell-is-on-grid grid run-x run-y))
                                               (nil? (get-in grid [run-x run-y])))
                                         ; If the cell's value is nil or this position is off the grid, the run is over.
                                         (reduced [run-length run-sum])
                                         ; Otherwise, record this cell's value and continue following the run.
                                         [(inc run-length)
                                          (+ run-sum (get-in grid [run-x run-y]))])))
                                   [0 0]
                                   (map inc (range))))
        [[x-length-1 x-sum-1] [x-length-2 x-sum-2]] [(run-in-direction -1 0) (run-in-direction 1 0)]
        [[y-length-1 y-sum-1] [y-length-2 y-sum-2]] [(run-in-direction 0 -1) (run-in-direction 0 1)]
        cell-value (get-in grid [x y])]

    [[(+ x-length-1 x-length-2 (if (nil? cell-value) 0 1))
      (+ x-sum-1 x-sum-2 cell-value)]
     [(+ y-length-1 y-length-2 (if (nil? cell-value) 0 1))
      (+ y-sum-1 y-sum-2 cell-value)]]))

(s/fdef find-runs
  :args (s/cat :grid ::sp/grid :cell ::sp/cell)
  :ret (s/cat :horizontal-run (s/spec ::sp/run) :vertical-run (s/spec ::sp/run)))

(defn cell-is-playable? [grid [x y]]
  (let [[[x-run-length _] [y-run-length _]] (find-runs grid x y)]
    (and (or (> x-run-length 0) (> y-run-length 0))
         (and (< x-run-length MAX-RUN-LENGTH) (< y-run-length MAX-RUN-LENGTH)))))

(defn find-playable-cells [grid]
  ; XXX this should return [[middle-cell-x middle-cell-y]] if the grid is empty
  (filter #(cell-is-playable? grid %) (find-empty-cells grid)))

(defn cell-is-blocked? [grid [x y]]
  (let [[[x-run-length _] [y-run-length _]] (find-runs grid x y)]
    (or (>= x-run-length MAX-RUN-LENGTH) (>= y-run-length MAX-RUN-LENGTH))))

(defn find-blocked-cells [grid]
  (filter #(cell-is-blocked? grid %) (find-empty-cells grid)))

(defn is-grid-valid? [grid]
  (every?
    identity
    (apply concat
           (for [x (range (count grid))]
             (for [y (range (count (grid x)))]
               (let [[[x-run-length x-run-sum] [y-run-length y-run-sum]] (find-runs grid x y)]
                 (and (<= 0 x-run-length MAX-RUN-LENGTH)
                      (<= 0 y-run-length MAX-RUN-LENGTH)

                      ; If this cell isn't part of a 2+ length run, its run's sum
                      ; doesn't have to be a multiple of 5.
                      ; e.g. if the only filled cells on the board have values [5 3 2],
                      ; and you're examining the cell with value 3 from an axis that's
                      ; perpendicular to the axis of that run, it's fine that 3 isn't a multiple of 5.
                      (or (<= x-run-length 1)
                          (= 0 (mod x-run-sum 5)))
                      (or (<= y-run-length 1)
                          (= 0 (mod y-run-sum 5))))))))))

(s/fdef is-grid-valid?
  :args (s/cat :grid ::sp/grid)
  :ret boolean?)
