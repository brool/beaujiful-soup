(ns beaujiful-soup.core
  ^{:author "Tim Lopez"
    :doc "Module to make HTML parsing easier." }
  (:require [pl.danieljanus.tagsoup :as tagsoup]
            [clojure.zip :as zip]
            [clojure.xml :as xml]
            [clojure.contrib.zip-filter :as zf]
            [clojure.contrib.zip-filter.xml :as zfx])
  (:use (clojure.contrib [def :only (defalias)]))
  (:import java.io.ByteArrayInputStream)
  (:import org.xml.sax.InputSource)
)

;; helper routine so we can use TagSoup to parse
(defn- startparse-tagsoup [s ch]
  (let [p (new org.ccil.cowan.tagsoup.Parser)]
      (. p (setContentHandler ch))
      (. p (parse s))))

(defn build-soup 
  "build-soup builds a zippered XML structure from anything that clojure.xml/parse can take."
  [u] 
     (zip/xml-zip (xml/parse u startparse-tagsoup)))

(defn build-string-soup 
  "Builds a zippered XML structure from a string."
  [s]
  (build-soup (InputSource. (ByteArrayInputStream. (.getBytes s "UTF-8")))))

;;
;; map some commonly used functions into the namespace
;;

(defalias descendants zf/descendants)
(defalias ancestors zf/ancestors)

(defalias node zip/node)
(defalias xml-> zfx/xml->)
(defalias xml1-> zfx/xml1->)

(defalias text= zfx/text=)
(defalias text zfx/text)
(defalias attr= zfx/attr=)
(defalias attr zfx/attr)
(defalias tag= zfx/tag=)
(defn tag [loc] (:tag (node loc)))

;; search current tree and below for the given ID
(defn id
  "Predicate for use with xml->.  Searches all children for the given ID."
  [v] 
  (fn [loc] (filter #(= (attr % :id) v) (descendants loc))))

(defn map-edit 
  "Given a function and a zipper, will apply zip/edit to every node of the tree."
  [f z]
  (loop [z0 (zip/next z)]
    (if 
        (zip/end? z0) (zip/root z0)
        (recur (zip/next (zip/edit z0 f)))
)))