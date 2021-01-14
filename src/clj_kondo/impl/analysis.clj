(ns clj-kondo.impl.analysis
  "Helpers for analysis output"
  {:no-doc true}
  (:refer-clojure :exclude [ns-name])
  (:require [clj-kondo.impl.utils :refer [assoc-some select-some]]))

(defn reg-usage! [ctx filename row col from-ns to-ns var-name arity lang in-def metadata]
  (let [analysis (:analysis ctx)]
    (when analysis
      (let [m (meta to-ns)
            to-raw (:raw-name m)
            to-ns (if (and to-raw (string? to-raw))
                    to-raw to-ns)]
        (swap! analysis update :var-usages conj
               (assoc-some
                (merge
                 {:filename filename
                  :row row
                  :col col
                  :from from-ns
                  :to to-ns
                  :name var-name}
                 (select-some metadata
                              [:private :macro
                               :fixed-arities
                               :varargs-min-arity
                               :deprecated
                               :name-row
                               :name-col
                               :name-end-row
                               :name-end-col]))
                :arity arity
                :lang lang
                :from-var in-def))))))

(defn reg-var! [{:keys [:analysis :base-lang :lang] :as _ctx}
                filename row col ns nom attrs]
  (when analysis
    (let [attrs (select-keys attrs [:private :macro :fixed-arities :varargs-min-arity
                                    :doc :added :deprecated :test :export :defined-by
                                    :name-row :name-col :name-end-col :name-end-row])]
      (swap! analysis update :var-definitions conj
             (assoc-some
              (merge {:filename filename
                      :row row
                      :col col
                      :ns ns
                      :name nom}
                     attrs)
              :lang (when (= :cljc base-lang) lang))))))

(defn reg-namespace! [{:keys [:analysis :base-lang :lang] :as _ctx}
                      filename row col ns-name in-ns? metadata]
  (when analysis
    (swap! analysis update :namespace-definitions conj
           (assoc-some
            (merge {:filename filename
                    :row row
                    :col col
                    :name ns-name}
                   metadata)
            :in-ns (when in-ns? in-ns?) ;; don't include when false
            :lang (when (= :cljc base-lang) lang)))))

(defn reg-namespace-usage! [{:keys [:analysis :base-lang :lang] :as _ctx}
                            filename row col from-ns to-ns alias metadata]
  (when analysis
    (let [m (meta to-ns)
          to-raw (:raw-name m)
          to-ns (if (and to-raw (string? to-raw))
                  to-raw to-ns)]
      (swap! analysis update :namespace-usages conj
             (assoc-some
               (merge {:filename filename
                       :row row
                       :col col
                       :from from-ns
                       :to to-ns}
                      metadata)
              :lang (when (= :cljc base-lang) lang)
              :alias alias)))))

(defn reg-local! [{:keys [:analysis] :as ctx} filename binding]
  (when analysis
    (swap! analysis update :locals conj
           (assoc-some (select-keys binding [:name :str :id :row :col :end-row :end-col :scope-end-col :scope-end-row])
                       :filename filename
                       :lang (when (= :cljc (:base-lang ctx)) (:lang ctx))))))

(defn reg-local-usage! [{:keys [:analysis] :as ctx} filename binding usage]
  (when analysis
    (swap! analysis update :local-usages conj
           (assoc-some (select-keys usage [:name :str :id :row :col :end-row :end-col])
                       :filename filename
                       :lang (when (= :cljc (:base-lang ctx)) (:lang ctx))
                       :id (:id binding)))))
