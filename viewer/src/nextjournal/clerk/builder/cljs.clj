(ns nextjournal.clerk.builder.cljs
  (:require [clojure.java.io :as io]
            [nextjournal.clerk :as clerk]
            nrepl.cmdline
            [shadow.cljs.devtools.api :as shadow]
            [shadow.cljs.devtools.config :as config]
            [shadow.cljs.devtools.server :as shadow.server]
            [shadow.cljs.devtools.server.runtime :as runtime])
  (:import (java.lang.management ManagementFactory)
           (java.util Locale)))

(defn get-config [{:as opts :keys [out-path] :or {out-path "public"}}]
  (-> (io/resource "nextjournal/clerk/shadow-cljs.edn")
      config/read-config
      (update-in [:builds :viewer :modules :viewer :entries] (fnil into []) (:extra-namespaces opts))
      (assoc-in [:builds :viewer :output-dir] (str out-path "/js"))
      (assoc-in [:builds :viewer :release :output-dir] (str out-path "/js"))
      config/normalize
      (->> (merge config/default-config))))

(defn get-build [opts]
  (-> (get-config opts)
      (config/get-build :viewer)))

(def default-release-opts
  {:compile-css true
   :bundle? false
   :out-path "public/build"})

(defn release
  "Builds a release of Clerk viewer, returns asset path."
  ([opts] (release opts {}))
  ([opts shadow-opts]
   (require 'shadow.cljs.silence-default-loggers)
   (let [opts (merge default-release-opts opts)
         server-running? (runtime/get-instance)
         _ (when-not server-running? (shadow.server/start! (get-config opts)))
         state (shadow/with-runtime
                (shadow/release*
                 (get-build opts)
                 shadow-opts))
         _ (when-not server-running? (shadow.server/stop!))
         output-name (->> (or (:shadow.build.closure/modules state)
                              (:build-modules state))
                          (filter (comp #{:viewer} :module-id))
                          first
                          :output-name)]
     (str "/js/" output-name))))

(def default-watch-opts {:out-path "public"})

(defn start-watch [serve-opts]

  (future (nrepl.cmdline/dispatch-commands {:middleware '[cider.nrepl/cider-middleware]}))
  (require 'shadow.cljs.silence-default-loggers)

  (shadow.server/start! (get-config serve-opts))
  (shadow/watch (get-build serve-opts))
  (clerk/serve! (assoc serve-opts :resource-urls {"/js/viewer.js" "/js/viewer.js"}))

  (set! *print-namespace-maps* false)
  (println "Clerk dev system ready in"
           (String/format (Locale. "en-US")
                          "%.2fs"
                          (to-array [(/ (.. ManagementFactory getRuntimeMXBean getUptime) 1000.0)]))))

(comment

 (do (shadow.cljs.devtools.api/stop-worker :viewer)
     (start {:extra-namespaces [] #_'[nextjournal.clerk.hello]}))
 )