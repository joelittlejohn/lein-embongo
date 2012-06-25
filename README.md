# lein-embongo _(emb[edded m]ongo)_

A Leiningen 2 plugin to run an 'embedded' instance of MongoDB during a build (e.g. for integration testing).

## Usage

Put `[lein-embongo "0.1.0"]` into the `:plugins` vector of your project.clj.

Invoke the embongo task, providing the name of some other task that should be run after starting MongoDB, e.g.

    $ lein embongo test

Once the task is complete, MongoDB will be stopped.

### Additional config
There are few optional config parameters that control how MongoDB runs:

```clojure
(defproject my-project "1.0.0-SNAPSHOT"
  :plugins [[lein-embongo "0.1.0-SNAPSHOT"]]
  :mongo-port 37017 ;optional, default 27017
  :mongo-version "2.0.4" ;optional, default 2.1.1
  :mongo-data-dir "/tmp/mongo-data-files" ;optional, default is a new dir in java.io.tmpdir
  :mongo-download-proxy-host "proxy.mycompany.com" ;optional, default is none
  :mongo-download-proxy-port 8080) ;optional, default 80
```

## License

Copyright © 2012 Joe Littlejohn

Distributed under the Eclipse Public License, the same as Clojure.
