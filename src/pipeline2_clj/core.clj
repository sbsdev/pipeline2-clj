(ns pipeline2-clj.core
  "Thin layer on top of the [Pipeline2 Web Service
  API](https://code.google.com/p/daisy-pipeline/wiki/WebServiceAPI)"
  (:require [clj-http.client :as client]
            [clj-http.util :refer [url-encode]]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.data.codec.base64 :as b64]
            [clojure.data.xml :as xml]
            [clojure.data.zip :as zf]
            [clojure.data.zip.xml :refer [attr xml-> xml1->]]
            [clojure.java.io :as io]
            [clojure.zip :refer [xml-zip]]
            [crypto.random :as crypt-rand]
            [pandect.core :as pandect])
  (:import [java.util.zip ZipEntry ZipOutputStream]))

(def ws-url "http://localhost:8181/ws")

(def ^:private iso-formatter (f/formatters :date-time-no-ms))
(def ^:private auth-id "clientid")
(def ^:private secret "supersekret")
(def ^:private remote false)

(def ^:private timeout 1000)
(def ^:private poll-interval 3000)

(defn- create-hash [message signing-key]
  (-> (pandect/sha1-hmac-bytes message signing-key)
      b64/encode
      String.))

(defn auth-query-params [uri]
  (let [timestamp (f/unparse iso-formatter (t/now))
        nonce (crypt-rand/base64 32)
        params {"authid" auth-id "time" timestamp "nonce" nonce}
        query-string (str uri "?" (client/generate-query-string params))
        hashcode (create-hash query-string secret)]
    {:query-params
     {"authid" auth-id "time" timestamp "nonce" nonce "sign" hashcode}}))

(defn job-sexp [script inputs options]
  (let [script-url (str ws-url "/script/" script)]
    [:jobRequest {:xmlns "http://www.daisy.org/ns/pipeline/data"}
     [:script {:href script-url}]
     (for [[port file] inputs]
       [:input {:name (name port)}
        [:item {:value (if-not remote
                         (str "file:" (url-encode file))
                         (url-encode (.getName (io/file file))))}]])
     (for [[key value] options]
       [:option {:name (name key)} value])]))

(defn job-request [script inputs options]
  (-> (job-sexp script inputs options)
      xml/sexp-as-element
      xml/emit-str))

(defn jobs []
  (let [url (str ws-url "/jobs")
        response (client/get url (auth-query-params url))]
    (when (client/success? response)
      (-> response :body xml/parse-str))))

(defn job [id]
  (let [url (str ws-url "/jobs/" id)
        response (client/get url (merge (auth-query-params url)
                                        #_{:socket-timeout timeout :conn-timeout timeout}))]
    (when (client/success? response)
      (-> response :body xml/parse-str))))

(defn- zip-files [files]
  (let [tmp-name (.getAbsolutePath (java.io.File/createTempFile "pipeline2-client" ".zip"))]
    (with-open [zip (ZipOutputStream. (io/output-stream tmp-name))]
      (doseq [f (map io/file files)]
        (.putNextEntry zip (ZipEntry. (.getName f)))
        (io/copy f zip)
        (.closeEntry zip)))
    tmp-name))

(defn- multipart-request [inputs body]
  {:multipart
   [{:name "job-data" :content (io/file (zip-files (vals inputs)))}
    {:name "job-request" :content body}]})

(defn job-create [script inputs options]
  (let [url (str ws-url "/jobs")
        request (job-request script inputs options)
        auth (auth-query-params url)
        body {:body request}
        multipart (multipart-request inputs request)
        response (client/post url (merge multipart auth))]
    (when (client/success? response)
      (-> response :body xml/parse-str))))

(defn job-result-1 [url]
  (let [response (client/get url (auth-query-params url))]
    (when (client/success? response)
      (-> response :body xml/parse-str))))

(defn job-result
  ([id]
     (job-result-1 (str ws-url "/jobs/" id "/result")))
  ([id type name]
     (job-result-1 (str ws-url "/jobs/" id "/result/" type "/" name)))
  ([id type name idx]
     (job-result-1 (str ws-url "/jobs/" id "/result/" type "/" name "/idx/" idx))))

(defn job-log [id]
  (let [url (str ws-url "/jobs/" id "/log")
        response (client/get url (auth-query-params url))]
    (when (client/success? response)
      (-> response :body))))

(defn job-delete [id]
  (let [url (str ws-url "/jobs/" id)
        response (client/delete url (auth-query-params url))]
    (client/success? response)))

(defn scripts []
  (let [url (str ws-url "/scripts")
        response (client/get url (auth-query-params url))]
    (when (client/success? response)
      (-> response :body xml/parse-str))))

(defn script [id]
  (let [url (str ws-url "/scripts/" id)
        response (client/get url (auth-query-params url))]
    (when (client/success? response)
      (-> response :body xml/parse-str))))

(defn alive? []
  (let [url (str ws-url "/alive")]
    (client/success? (client/get url))))

(defn- get-id [job]
  (-> job xml-zip (xml1-> (attr :id))))

(defn- get-status [job]
  (-> job xml-zip (xml1-> (attr :status))))

(defn get-results [job]
  ;; unfortunately `(xml-> :results :result :result (attr :href))`
  ;; doesn't work here as `xml->` has a problem with that, see
  ;; https://dev.clojure.org/jira/browse/DZIP-6
  (-> job xml-zip (xml-> :results :result zf/children (attr :href))))

(defn get-stream [url]
  (let [response (client/get url (merge (auth-query-params url) {:as :stream}))]
    (when (client/success? response)
      (-> response :body))))

(defn create-job-and-wait [script inputs options]
  (let [id (get-id (job-create script inputs options))]
    (Thread/sleep poll-interval) ; wait a bit before polling the first time
    (loop [result (job id)]
      (let [status (get-status result)
            _ (println "Status: " status)]
        (if (= "RUNNING" status)
          (do
            (Thread/sleep poll-interval)
            (recur (job id)))
          result)))))
