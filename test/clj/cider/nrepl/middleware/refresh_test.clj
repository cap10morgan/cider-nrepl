(ns cider.nrepl.middleware.refresh-test
  (:require [cider.nrepl.test-session :as session]
            [clojure.test :refer :all]))

(use-fixtures :each session/session-fixture)

(def ^:private dirs-to-reload
  ;; Limit the scope of what we reload, because (for example) reloading the
  ;; cider.nrepl.middleware.test-session ns causes *session* in that ns to be
  ;; unloaded, which breaks session-fixture, and hence all of the below tests.
  ["test/clj/cider/nrepl/middleware/util"])

(defn- before-fn []
  (println "before-fn invoked"))

(defn- after-fn []
  (println "after-fn invoked"))

(deftest refresh-op
  (testing "refresh op works"
    (let [response (session/message {:op "refresh"
                                     :dirs dirs-to-reload})]
      (is (:reloading response))
      (is (= #{"done" "ok"} (:status response)))))

  (testing "nothing to refresh after refreshing"
    (let [response (session/message {:op "refresh"
                                     :dirs dirs-to-reload})]
      (is (= [] (:reloading response)))
      (is (= #{"done" "ok"} (:status response))))))

(deftest before-arg
  (testing "before arg works"
    (let [response (session/message {:op "refresh"
                                     :dirs dirs-to-reload
                                     :before "cider.nrepl.middleware.refresh-test/before-fn"})]
      (is (:reloading response))
      (is (= #{"done" "invoked-before" "invoking-before" "ok"} (:status response)))
      (is (= "before-fn invoked\n" (:out response)))))

  (testing "bad before arg results in error"
    (let [response (session/message {:op "refresh"
                                     :dirs dirs-to-reload
                                     :before "foo"})]
      (is (= #{"done" "error" "invoking-before"} (:status response)))
      (is (:err response))
      (is (:error response)))

    (let [response (session/message {:op "refresh"
                                     :dirs dirs-to-reload
                                     :before "clojure.core/seq"})]
      (is (= #{"done" "error" "invoking-before"} (:status response)))
      (is (:err response))
      (is (:error response)))

    (let [response (session/message {:op "refresh"
                                     :dirs dirs-to-reload
                                     :before "java.lang.Thread"})]
      (is (= #{"done" "error" "invoking-before"} (:status response)))
      (is (:err response))
      (is (:error response)))))

(deftest after-arg
  (testing "after arg works"
    (let [response (session/message {:op "refresh"
                                     :dirs dirs-to-reload
                                     :after "cider.nrepl.middleware.refresh-test/after-fn"})]
      (is (:reloading response))
      (is (= #{"done" "invoked-after" "invoking-after" "ok"} (:status response)))
      (is (= "after-fn invoked\n" (:out response)))))

  (testing "bad after arg results in error"
    (let [response (session/message {:op "refresh"
                                     :dirs dirs-to-reload
                                     :after "foo"})]
      (is (= #{"done" "error" "invoking-after" "ok"} (:status response)))
      (is (:err response))
      (is (:error response)))

    (let [response (session/message {:op "refresh"
                                     :dirs dirs-to-reload
                                     :after "clojure.core/seq"})]
      (is (= #{"done" "error" "invoking-after" "ok"} (:status response)))
      (is (:err response))
      (is (:error response)))

    (let [response (session/message {:op "refresh"
                                     :dirs dirs-to-reload
                                     :after "java.lang.Thread"})]
      (is (= #{"done" "error" "invoking-after" "ok"} (:status response)))
      (is (:err response))
      (is (:error response)))))

(deftest refresh-all-op
  (testing "refresh-all op works"
    (let [response (session/message {:op "refresh-all"
                                     :dirs dirs-to-reload})]
      (is (seq (:reloading response)))
      (is (= #{"done" "ok"} (:status response))))))

(deftest refresh-clear-op
  (testing "refresh-clear op works"
    (let [_ (session/message {:op "refresh"
                              :dirs dirs-to-reload})
          response (session/message {:op "refresh-clear"})]
      (is (= #{"done"} (:status response)))))

  (testing "refresh op works after refresh clear"
    (let [response (session/message {:op "refresh"
                                     :dirs dirs-to-reload})]
      (is (seq (:reloading response)))
      (is (= #{"done" "ok"} (:status response))))))
