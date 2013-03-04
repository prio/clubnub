(ns clubnub.core-test
  (:use midje.sweet
        clubnub.core
        [midje.util :only [testable-privates]])
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))


(def creds {:pub-key "pub", :sub-key "sub"})

(def sub-url "http://pubsub.pubnub.com/subscribe/sub/demo/0/")
(def pub-url "http://pubsub.pubnub.com/publish/pub/sub/demo/0/")


(testable-privates clubnub.core build-url)


(tabular
 (fact "get-url-parts works for publish"
       (get-url-parts :publish ?config "demo" ?msg) => ?expected
       (provided (json/generate-string ..msg..) => "msg"))
 ?config                                    ?msg     ?expected
 (assoc creds :secret-key "sec")            ..msg..  ["publish" "pub" "sub" "sec" "demo" "0" "msg"]
 (assoc creds :ssl true)                    ..msg..  ["publish" "pub" "sub"  "demo" "0" "msg"]
 (assoc creds :origin "poppy.pubnub.com")   ..msg..  ["publish" "pub" "sub"  "demo" "0" "msg"])


(tabular
 (fact "get-url-parts works for subscribe"
       (get-url-parts :subscribe ?config "demo" ?time) => ?expected)
 ?config                                    ?time    ?expected
 (assoc creds :secret-key "sec")            0        ["subscribe"  "sub"  "demo" "0" 0]
 (assoc creds :ssl true)                    123      ["subscribe"  "sub"  "demo" "0" 123]
 (assoc creds :origin "poppy.pubnub.com")   123.456  ["subscribe"  "sub"  "demo" "0" 123.456])


(tabular
 (fact "build-url returns a valid subscribe url"
       (build-url :subscribe ?config "demo" ?time) => ?url)
 ?config                                    ?time    ?url
 (assoc creds :secret-key "sec")            0        "http://pubsub.pubnub.com/subscribe/sub/demo/0/0"
 (assoc creds :ssl true)                    123      "https://pubsub.pubnub.com/subscribe/sub/demo/0/123"
 (assoc creds :origin "poppy.pubnub.com")   123.456  "http://poppy.pubnub.com/subscribe/sub/demo/0/123.456")


(fact "build-url returns a valid publish url"
        (build-url :publish creds "demo" {:msg "hi" })
        => (str pub-url "%7B%22msg%22:%22hi%22%7D")
        (build-url :publish creds "demo" {:msg "bye", :id 1})
        => (str pub-url  "%7B%22msg%22:%22bye%22,%22id%22:1%7D"))

(comment
  (fact "poll works"
        (poll creds "demo") => {:msg "hello"}
        (provided (http/get (str pub-url "0")) => {:body "{\"msg\":\"hello\"}"})))
