# lein-embongo _(emb[edded m]ongo)_

A Leiningen 2 plugin to run an 'embedded' instance of MongoDB during a build (e.g. for integration testing).

The Mongo instance isn't strictly embedded (it's not running within the JVM of your application or lein), but it _is_ a managed instance that exists only for the lifetime of your build.

## Usage

Add `[lein-embongo "0.2.2"]` to the `:plugins` vector of your project.clj.

Invoke the embongo task, providing the name of some other task that should be run after starting MongoDB, e.g.

    $ lein embongo test

Once the task is complete, MongoDB will be stopped.

If for some reason you'd like lein to start the embedded mongo instance and wait, you can invoke the `embongo` task without providing any further tasks, like:

    $ lein embongo

When you want to stop MongoDB just press <kbd>Ctrl</kbd>+<kbd>C</kbd>.

### Additional config
There are a few optional config parameters that control how MongoDB runs:

```clojure
(defproject my-project "1.0.0-SNAPSHOT"
  :plugins [[lein-embongo "0.2.2"]]
  :embongo {:port 37017 ;optional, default 27017
            :version "2.1.1" ;optional, default 2.4.3
            :data-dir "/tmp/mongo-data-files" ;optional, default is a new dir in java.io.tmpdir
            :download-proxy-host "proxy.mycompany.com" ;optional, default is none
            :download-proxy-port 8080} ;optional, default 80
```

## Notes

* All mongod output appears in `./embongo.log`
* If you want to run many lein builds in parallel using Jenkins, try the [Port Allocator Plugin](https://wiki.jenkins-ci.org/display/JENKINS/Port+Allocator+Plugin) to avoid port conflicts. If you assign a port to $MONGO_PORT, you can set the `:port` config option for embongo like:

```clojure
(defproject my-project "1.0.0-SNAPSHOT"
  :embongo {
  :port ~(Integer. (get (System/getenv) "MONGO_PORT" 27017)) ;uses port 27017 if env var is not set
  ...
```

## License

Copyright © 2017 Joe Littlejohn

Distributed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html), the same as Clojure.
