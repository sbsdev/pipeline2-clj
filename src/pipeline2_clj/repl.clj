(ns pipeline2-clj.repl
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [pipeline2-clj.scripts :as scripts]
            [slingshot.slingshot :refer [throw+ try+]]))

(defn remove-extension [file]
  (let [file-name (.getName file)]
    (subs file-name 0 (string/last-index-of file-name "."))))

(defn epub-name [file export-path]
  (as-> file f
    (remove-extension f)
    (str f ".epub")
    (io/file export-path f)
    (.getAbsolutePath f)))

(defn convert [xml epub]
  (with-open [in (scripts/dtbook-to-epub3 xml)
              out (io/output-stream epub)]
    (io/copy in out)))

(defn convert-safe [xml epub]
  (try+
   (convert xml epub)
   (catch [:status 403] {:keys [request-time headers body]}
     (log/warn "403" request-time headers))
   (catch [:status 404] {:keys [request-time headers body]}
     (log/warn "NOT Found 404" request-time headers body))
   (catch [:status 500] {:keys [request-time headers body]}
     (log/warn "Server error" request-time headers body))
   (catch Object _
     (log/error (:throwable &throw-context) "unexpected error")
     (throw+))))

(defn convert-all [path export-path]
  (let [ebooks (->> path io/file file-seq (filter #(.isFile %)))]
    (doseq [ebook ebooks]
      (let [ebook-name (.getAbsolutePath ebook)
            epub-name (epub-name ebook export-path)]
        (println "Ebook:" ebook-name)
        (println "EPUB:" epub-name)
        (try+
         (with-open [in (scripts/dtbook-to-epub3 ebook-name)
                     out (io/output-stream epub-name)]
           (io/copy in out)) 
         (catch [:status 403] {:keys [request-time headers body]}
           (log/warn "403" request-time headers))
         (catch [:status 404] {:keys [request-time headers body]}
           (log/warn "NOT Found 404" request-time headers body))
         (catch [:status 500] {:keys [request-time headers body]}
           (log/warn "Server error" request-time headers body))
         (catch Object _
           (log/error (:throwable &throw-context) "unexpected error")
           (throw+)))))))
