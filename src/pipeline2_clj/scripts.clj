(ns pipeline2-clj.scripts
  "Thin layer above the [Pipeline2 Web Service
  API](https://code.google.com/p/daisy-pipeline/wiki/WebServiceAPI) to
  invoke specific scripts."
  (:require [clojure.string :as string]
            [pipeline2-clj.core :as dp2]))

(defn validate [input & {:keys [mathml-version check-images] :as opts}]
  (dp2/create-job-and-wait "dtbook-validator" {} (merge opts {:input-dtbook input})))

(defn daisy3-to-epub3 [input & {:keys [mediaoverlays assert-valid] :as opts}]
  (dp2/create-job-and-wait "daisy3-to-epub3" {:source input} opts))

(defn epub3-to-daisy202 [input & {:keys [temp-dir output-dir] :as opts}]
  (dp2/create-job-and-wait "epub3-to-daisy202" {} (merge {:epub input} opts)))

(defn dtbook-to-epub3 [input]
  (->>
   (dp2/create-job-and-wait "sbs:dtbook-to-ebook" {:source input} {})
   (dp2/get-results)
   (filter #(string/ends-with? % ".epub"))
   first
   dp2/get-stream))

