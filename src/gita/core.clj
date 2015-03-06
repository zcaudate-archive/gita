(ns gita.core
  (:require [gita.api.commands :as commands]
            [gita.api.repository :as repository]
            [gita.interop :as interop])
  (:import org.eclipse.jgit.api.Git))

(def ^:dynamic *dir* nil)

(defn git-help [all-commands]
  (let [out (-> all-commands keys sort vec)]
    (println "\nSubtasks for git are:\n\n")
    (println out)
    out))

(defn git-command-help [cmd]
  (let [opts (reduce-kv (fn [m k res]
                          (assoc m k (commands/command-input res)))
                        {} (commands/command-options cmd))]
    (println "Options are: " opts)
    opts))

(defn wrap-help [f]
  (fn  [cmd inputs]
    (if (some #{:? :help} inputs)
      (git-command-help cmd)
      (f cmd inputs))))

(defn wrap-result [f]
  (fn [cmd inputs]
    (let [res (->> (filter #(not= :& %) inputs)
                   (f cmd))]
      (if (some #{:&} inputs)
        res
        (interop/to-data res)))))

(defn run-base [cmd inputs]
  (-> cmd
      (commands/command-initialize-inputs inputs)
      (.call)))

(defn run-command [pair dir]
  (if (vector? pair)
    (let [[ele inputs] pair
          cmd (if (-> ele :modifiers :static)
                (ele)
                (ele (Git. (repository/repository dir))))]

      ((-> run-base
           wrap-result
           wrap-help) cmd inputs))
    pair))

(defn git
  ([] (git :help))
  ([dir? & args]
   (let [all-commands (commands/git-all-commands)
         curr (System/getProperty "user.dir")
         [dir [c & cs :as args]] (if (keyword? dir?)
                                    [(or *dir* curr)
                                     (cons dir? args)]
                                    [(do (alter-var-root #'*dir* (fn [x] dir?))
                                         dir?)
                                     args])]
     (cond (= :help c)
           (git-help all-commands)

           (= :cd c)
           (alter-var-root #'*dir* (fn [x] (first cs)))

           (= :pwd c)
           (or *dir* curr)

           :else
           (-> (commands/command all-commands args)
               (run-command dir))))))


(comment
  (git)
  (git :cd)
  (git :pwd)
  (git :init :?)
  ;; => {:git-dir java.lang.String, :directory java.lang.String, :bare boolean}

  (git :add :?)
  (git :rm :?)
  (git :stash :create)
  (git :pwd)
  "/tmp/gita-example"
  "/Users/chris/Development/chit/gita"

  (git :init :directory "/tmp/gita-example")
  => "/tmp/gita-example/.git"

  (git :cd "/tmp/gita-example")
  (git :cd "/tmp/gita-example1")
  (git :status)
  (iterator-seq (.iterator (git :log)))

  (type (.getAuthor (.next (git :log)))

        (type (.getEncoding (.next (git :log))))
        sun.nio.cs.UTF_8
        (-> (git :log) (.iterator) (iterator-seq))

        (.getAuthorIdent (.next (git :log))))


  (git :add :?)
  (git :cd)
  (git :push :remote "git@github.com:zcaudate/gita.git")
  (def res (git :pull :&))

  (interop/to-data res)

  (git :init :?)
  (git :status)





  (spit "/tmp/gita-example/hello.txt" "hello there")

  (git :add :filepattern ["."])
  (git :rm :help)
  (git :status)
  (git :commit :message "basic commit")
  (git "/tmp/gita/init" :status)
  (git "." :status)
  (git :branch :create))