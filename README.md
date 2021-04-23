# pipeline2-clj

A Clojure library to interact with the [Pipeline2 Web Service
API](https://daisy.github.io/pipeline/WebServiceAPI).

## Usage

```clojure
(require '[clojure.java.io :as io])
(require '[clojure.data.xml :as xml])

;; list all script supported by the server
(print (xml/indent-str (scripts)))

;; get help for a particular script
(print (xml/indent-str (script "dtbook-to-epub3")))

;; invoke a script
(-> (create-job-and-wait "dtbook-to-epub3" {:source "dtbook-file.xml"} {})
    get-results
    first
    get-stream
    (io/copy (io/file "book.epub")))
```
## License

Copyright © 2021 Swiss Library for the Blind, Visually Impaired and Print Disabled

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
