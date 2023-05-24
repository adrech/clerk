(ns nextjournal.clerk.view
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [hiccup.page :as hiccup]
            [nextjournal.clerk.viewer :as v])
  (:import (java.net URI)))


(defn ^:private extract-name->viewer [presentation]
  (into {}
        (map (juxt (fn [viewer]
                     (or (when (qualified-symbol? (:name viewer))
                           (symbol (namespace (:name viewer)) (str (name (:name viewer)) "$" (:hash viewer))))
                         (symbol "nextjournal.clerk.viewer" (str "viewer-fn$" (:hash viewer))))) identity))
        (keep :nextjournal/viewer (tree-seq (some-fn map? vector?) #(cond-> % (map? %) vals) presentation))))

(defn +name->viewer [presentation]
  (let [name->viewer (extract-name->viewer presentation)]
    (assoc (walk/postwalk-replace (set/map-invert name->viewer) presentation)
           :name->viewer name->viewer)))

(defn doc->viewer
  ([doc] (doc->viewer {} doc))
  ([opts {:as doc :keys [ns file]}]
   (binding [*ns* ns]
     (-> (merge doc opts) v/notebook v/present +name->viewer))))

#_(doc->viewer (nextjournal.clerk/eval-file "notebooks/hello.clj"))
#_(nextjournal.clerk/show! "notebooks/test.clj")
#_(nextjournal.clerk/show! "notebooks/visibility.clj")

(defn relative? [url]
  (and (not (.isAbsolute (URI. url)))
       (not (str/starts-with? url "/"))))

#_(relative? "/hello/css")
#_(relative? "hello/css")
#_(relative? "https://cdn.stylesheet.css")

(defn adjust-relative-path [{:as state :keys [current-path]} url]
  (cond->> url
    (and current-path (relative? url))
    (str (v/relative-root-prefix-from (v/map-index state current-path)))))

(defn include-viewer-css [state]
  (if-let [css-url (get-in state [:resource->url "/css/viewer.css"])]
    (hiccup/include-css (adjust-relative-path state css-url))
    (list (hiccup/include-js "https://cdn.tailwindcss.com?plugins=typography")
          [:script (-> (slurp (io/resource "stylesheets/tailwind.config.js"))
                       (str/replace #"^module.exports" "tailwind.config")
                       (str/replace #"require\(.*\)" ""))]
          [:style {:type "text/tailwindcss"} (slurp (io/resource "stylesheets/viewer.css"))])))

(defn include-css+js [state]
  (list
   (include-viewer-css state)
   [:script {:type "module" :src (adjust-relative-path state (get-in state [:resource->url "/js/viewer.js"]))}]
   (hiccup/include-css "https://cdn.jsdelivr.net/npm/katex@0.13.13/dist/katex.min.css")
   [:link {:rel "preconnect" :href "https://fonts.bunny.net"}]
   (hiccup/include-css "https://fonts.bunny.net/css?family=fira-mono:400,700%7Cfira-sans:400,400i,500,500i,700,700i%7Cfira-sans-condensed:700,700i%7Cpt-serif:400,400i,700,700i")))

(defn escape-closing-script-tag [s]
  ;; we must escape closing `</script>` tags, see
  ;; https://html.spec.whatwg.org/multipage/syntax.html#cdata-rcdata-restrictions
  (str/replace s "</script>" "</nextjournal.clerk.view/escape-closing-script-tag>"))

(defn ->html [{:as state :keys [conn-ws? current-path html exclude-js?]}]
  (hiccup/html5
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    (when current-path (v/open-graph-metas (-> state :path->doc (get current-path) v/->value :open-graph)))
    (if exclude-js?
      (include-viewer-css state)
      (include-css+js state))]
   [:body.dark:bg-gray-900
    [:div#clerk html]
    (when-not exclude-js?
      [:script {:type "module"} "let viewer = nextjournal.clerk.sci_env
let state = " (-> state v/->edn escape-closing-script-tag pr-str) ".replaceAll('nextjournal.clerk.view/escape-closing-script-tag', 'script')
viewer.init(viewer.read_string(state))\n"
       (when conn-ws?
         "viewer.connect(document.location.origin.replace(/^http/, 'ws') + '/_ws')")])]))
