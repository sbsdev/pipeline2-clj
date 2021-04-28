(ns pipeline2-clj.repl
  (:require [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [pipeline2-clj.core :as dp2]))

(defn epub-name [export-path file]
  (-> file
      fs/split-ext
      first
      (str ".epub")
      (->> (fs/path export-path))))

(defn to-epub [dtbook epub]
  (dp2/with-job [job (dp2/job-create "sbs:dtbook-to-ebook" {:source dtbook} {})]
    (let [completed (dp2/wait-for-result job)
          results (dp2/get-results completed)
          epub-stream (->> results
                           (filter #(string/ends-with? % ".epub"))
                           first
                           dp2/get-stream)]
      (with-open [in epub-stream
                  out (io/output-stream epub)]
        (io/copy in out)))))

(defn convert-all [converter in-path out-path]
  (doseq [dtbook (fs/glob in-path "*.xml")]
    (let [dtbook (str dtbook)
          epub (str (epub-name out-path dtbook))]
      (println "Ebook:" dtbook)
      (println "EPUB:" epub)
      (try
        (converter dtbook epub)
        (catch Exception e (println (ex-message e)))))))

