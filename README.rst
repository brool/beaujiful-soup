Beaujiful Soup
==============

`Beautiful Soup`_ is a really nice Python library for extracting
content from possibly-sloppy HTML, and I wanted some reasonably close
Clojure equivalent.  Unfortunately, the standard classes don't work
well malformed HTML; as an example::

    => (require '(clojure [xml :as xml]))
    => (xml/parse "http://www.google.com")
    org.xml.sax.SAXParseException: The markup in the document preceding the root element must be well-formed. (NO_SOURCE_FILE:0)

Fortunately, there is already a `TagSoup`_ library that can parse
non-perfect HTML, and it is very `easy to integrate`_ TagSoup into
xml/parse.  This module hardly does anything; it simply adds a few
helper routines and brings the most-used calls into one amazingly bad
namespace name.

.. _Beautiful Soup: http://www.crummy.com/software/BeautifulSoup/
.. _TagSoup: http://home.ccil.org/~cowan/XML/tagsoup/
.. _easy to integrate: http://markmail.org/message/2e7i72y4cg36wqdx

Examples
--------

Building your soup::

    (use beaujiful-soup.core)
    
    ; build soup from URL
    (def t (build-soup "http://www.google.com"))

    ; build soup from (deliberately malformed) string
    (def t2 (build-string-soup "<html><body><ul><li>One<li>Two</ul></body></html>"))

Extracting information is done with the ``xml->`` call. Oftentimes the last thing you do will be a ``node`` or ``text`` or ``(attr :attribute)`` call, in order to convert the results into a more workable type::

    ; you can "walk" down the tree with successive tag names.  For
    ; example, get every list item inside the unordered list
    ; immediately inside the body.
    (xml-> t2 :body :ul :li node)
    ; => ({:tag :li, :attrs nil, :content ["One"]} {:tag :li, :attrs nil, :content ["Two"]})

    ; get the text for the list items
    (xml-> t2 :body :ul :li text)
    ; => ("One" "Two")

    ; Get textareas immediately inside the body.
    (xml-> t :body :textarea node)
    ; => ({:tag :textarea, :attrs {:id "csi", :style "display:none"}, :content nil})

    ; use descendants to iterate through all nodes, not just the immediate children.
    ; Get the text from all <a> tags anywhere in the body.
    (xml-> t descendants :a text)
    ; => ("Images" "Videos" "Maps" ...)

    ;  Get the href attribute from all tags
    (xml-> t descendants :a (attr :href))
    ; => ("http://www.google.com/imghp?hl=en&tab=wi" ... )

Use the (attr=) predicate to match an attribute value::

    ; find invisible stuff
    (xml-> t2 descendants (attr= :style "display:none") tag)
    ; => (:textarea :iframe)    

Strings match the text inside nodes::

    ; find the link for the <a> that has "Videos" for content
    (xml-> t descendants :a "Videos" (attr :href))
    ; => ("http://video.google.com/?hl=en&tab=wv")

Arbitrary predicates can be used as well.  They will take a loc (location), and are usually converted to a node before being used::

    ; find any :p or :div
    (defn p-or-div [loc] (contains? #{:p :div} (:tag (node loc))))
    (xml-> t descendants p-or-div tag)
    ; => (:div :div :div :div :div :div :div :div :div :div :div :p :div :div)

    ; find the link for <a> that has case-insensitive "Videos" for content
    (require 'clojure.string)
    (defn f [loc] 
      (let [n (node loc)]
       (and (= (:tag n) :a) (= (clojure.string/upper-case (first (:content n))) "VIDEOS"))))
    (xml-> t descendants f (attr :href))
    ; => ("http://video.google.com/?hl=en&tab=wv")

Fundamentally, the xml-> call returns a list of locations, and you can apply arbitrary transforms as necessary.  For example, let's say that you want to build a map of text => href links based on the bookmarks::

    (defn loc-to-pair [loc]
        [ (attr loc :href), (text loc) ])
    (apply hash-map (xml-> t descendants :a loc-to-pair))
    ; => {"/services/" "Business Solutions",  ... }

Having a vector in the chain applies all the predicates within the vector, and filters out anything that doesn't match.  It acts a little like a lookahead in a regex.  For example::

   ; Find the IDs of all divs that contain an href immediately within them
   (xml-> t descendants :div [ :a ] (attr :id))
    ; => ("fll")

    ; Find the IDs of all divs that contains an href anywhere within them
    (xml-> t descendants :div [ descendants :a ] (attr :id))
    ; => ("ghead" "gbar" "guser" "fll")

