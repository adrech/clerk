;; # 🪟 Windows
(ns notebook.tap-window
  {:nextjournal.clerk/visibility {:code :hide :result :hide}
   :nextjournal.clerk/no-cache true}
  (:require [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as v]))

(clerk/window! ::clerk/taps)

(comment
  (tap> (clerk/html [:div.w-8.h-8.bg-green-500]))
  (tap> (clerk/plotly {:data [{:x [1 2 3 4]}]})))
