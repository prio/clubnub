(ns clubnub.core
  (:import [java.net SocketException])
  (:use [digest :only [digest]])
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.string :as string]))


(def default-origin "pubsub.pubnub.com")


(defn- sign [channel message pub-key sub-key secret-key]
  (if secret-key
    (->> [pub-key sub-key secret-key channel]
         (string/join "/")
         (digest "md5"))
    "0"))


(defn- encode-path-segment [segment]
  (-> (java.net.URI. nil nil (str "/" segment) nil)
      (str)
      (.substring 1)))


(defmulti get-url-parts
  (fn [type & more] type))

(defmethod get-url-parts :publish [type config channel msg]
  (let [{:keys [pub-key sub-key secret-key]} config
        secret (or secret-key "0")]
    ["publish" pub-key sub-key secret channel "0" (json/generate-string msg)]))

(defmethod get-url-parts :subscribe [type config channel timetoken]
  ["subscribe" (:sub-key config) channel "0" timetoken])



(defn- build-url [type config channel param]
  (let [{:keys [pub-key sub-key secret-key origin ssl]} config
        transport (if ssl "https" "http")
        host (or origin default-origin)
        parts (get-url-parts type config channel param)]
    (str transport "://"
         (->> (cons host parts)
              (map encode-path-segment)
              (string/join "/")))))


(defn publish [config channel message]
  "Publishes the supplied message to the specified chanel.
   Config must contain at least a :pub-key and a :sub-key"
  ({:pre [(string? channel)
          (every? config [:pub-key :sub-key])]}
   (let [uri (build-url :publish config channel message)]
     (http/get uri))))


(defn- poll
  "Connects to pubnub with the specified configuration and
   returns all the new messages sent on the channel since
   timetoken"
  ([config channel]
     (poll config channel 0))
  ([config channel timetoken]
     (let [uri (build-url :subscribe config channel timetoken)]
       (try
         (json/parse-string (:body (http/get uri)) keyword)
         (catch SocketException e
           [[] timetoken])))))


(defn subscribe
  "Subscribes to the spcified pubnub channel and returns a
   future that can be cancelled at any time.

   Currently accepts the following optionl parameters:
    * callback: Called for every msg recieved on channel, callback function
      recieves the reading as its parameter
    * connect: Called when initial connection is made with pubnub"
  [config channel & {:keys [callback connect]}]
  ({:pre [(string? channel)
          (every? config [:sub-key])]}
   (let [initial-timetoken (second (poll config channel))]
     (if connect (connect))
     (when callback
       (future
         (loop [[msgs timetoken] (poll config channel initial-timetoken)]
           (if (seq msgs)
             (doseq [msg msgs]
               (callback msg))
             (Thread/sleep 100))
           (recur (poll config channel timetoken))))))))
