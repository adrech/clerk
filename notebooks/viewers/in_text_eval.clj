;; #  📝 _In-Text_ Evaluation
^{:nextjournal.clerk/visibility #{:hide-ns}}
(ns ^{:nextjournal.clerk/no-cache true} viewers.in-text-eval
  (:require [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as v]
            [viewers.custom-markdown :as custom-md]
            [nextjournal.markdown.transform :as markdown.transform]))

;; Being able to override markdown viewers allows us to get in-text evaluation for free:

(defonce num★ (atom 3))
#_(reset! num★ 3)

^{::clerk/visibility :hide ::clerk/viewer :hide-result}
(custom-md/update-markdown-viewers!
 (fn [mdvs] (v/add-viewers mdvs [{:name :nextjournal.markdown/monospace
                                  :transform-fn (comp eval read-string markdown.transform/->text)}
                                 {:name :nextjournal.markdown/ruler
                                  :transform-fn (constantly (v/with-viewer :html [:div.text-center (repeat @num★ "★")]))}])))

;; ---
^{::clerk/viewer clerk/hide-result ::clerk/visibility :hide}
(defn slider [var {:keys [min max]}]
  (clerk/with-viewer
    {:fetch-fn (fn [_ x] x)
     :transform-fn (fn [var] {:var-name (symbol var) :value @@var})
     :render-fn `(fn [{:as x :keys [var-name value]}]
                   (v/html [:input {:type :range
                                    :min ~min :max ~max
                                    :value value
                                    :on-change #(v/clerk-eval `(reset! ~var-name (Integer/parseInt ~(.. % -target -value))))}]))}
    var))

;; Drag the following slider `(slider #'num★ {:min 1 :max 40})` to control the number of stars (currently **`(deref num★)`**) in our custom horizontal rules.

;; ---
