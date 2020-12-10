## Pain Points to be aware of

* TripleA is VERY brittle, be really careful and test your changes. We like to
  see good automated tests as much as possible to help us going forward and
  reduce the amount of we have to test. Try to make sure your changes are super
  solid so we can be confident every change is a step forward and we are not
  spending lots of time debugging obsure errors when it comes to release.

* TripleA seems simple at first, but it's very much tangled in knots in
  many places. To fix one thing, you often need to fix another, and to fix
  that you need to fix perhaps the first thing and something else.

* Save game and/or network compatibility can be quite easy to break,
  be aware of how serialization is done for save games. Unless we are in
  a major release cycle, keeping compatibility can be really constraining.
