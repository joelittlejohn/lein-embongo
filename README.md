# lein-embongo _(emb[edded m]ongo)_

A Leiningen plugin to run an 'embedded' instance of MongoDB during a build (e.g. for integration testing).

## Usage

Put `[lein-embongo "0.1.0"]` into the `:plugins` vector of your project.clj.

Invoke the embongo task, providing the name of some other task that should be run after starting MongoDB, e.g.

    $ lein embongo test

Once the task is complete, MongoDB will be stopped.

## License

Copyright © 2012 Joe Littlejohn

Distributed under the Eclipse Public License, the same as Clojure.
