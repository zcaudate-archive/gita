(ns gita.interop.dir-cache-test
  (:require [gita.interop.helpers :refer :all]
            [gita.interop :as interop]
            [hara.object :as object]
            [clojure.java.io :as io]
            [midje.sweet :refer :all]))

(fact "testing dir-cache and dir-cache-entry"
  (let [path (str "/tmp/gita/" (java.util.UUID/randomUUID))
        tempdir (io/file path)]
    (git-status-call tempdir)
    (spit (str path "/hello.txt") "hello")
    (-> (git-add-call tempdir)
        (object/to-data))
    => {"hello.txt" #{:smudged :merged}}

    (-> (git-add-call tempdir)
        (.getEntry 0)
        (object/to-data)))
  => (contains {:merged? true,
                :file-mode "100644",
                :stage 0,
                :object-id string?
                :last-modified number?
                :length number?,
                :path-string "hello.txt",
                :smudged? true,
                :creation-time 0}))
