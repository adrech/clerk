;; # 🪟 Windows
(ns tap-window
  {:nextjournal.clerk/visibility {:code :hide :result :hide}
   :nextjournal.clerk/no-cache true}
  (:require [nextjournal.clerk :as clerk]
            [nextjournal.clerk.window :as window]
            [nextjournal.clerk.tap :as tap]))

#_(clerk/window! :my-window (clerk/html [:div.w-8.h-8.bg-green-500]))

#_(comment
    (clerk/close-window! :my-window)
    (clerk/close-all-windows!)
    (clerk/window! ::clerk/taps)
    (tap> (clerk/html [:div.w-8.h-8.bg-green-500]))
    (tap> (clerk/plotly {:data [{:x [1 2 3 4]}]}))
    (tap/reset-taps!))
