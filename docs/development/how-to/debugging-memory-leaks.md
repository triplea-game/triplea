# Debugging Memory Leaks

To debug and fix memory leaks in TripleA, a tool that can generate a heap dump, like VisualVM can be used.

Here are some useful things you can do with VisualVM:

  1. Find which objects are taking a lot of space. To do so, take a heap dump (right click on a process, select "Heap Dump") and
     switch to the Objects view.
  2. Look at specific instances of an object. To do so, from the Objects view, click the `[+]` to view instances.
  3. Find references to an object. When an object is expanded, click on `<references>` in the tree. You can then find references to those
     references by continuing to expand the tree.
  4. Find the "GC Root" for an object. This can tell why this object is being retained. This may identify a static reference or some other
     object that can be a "GC Root" in Java, such as a Frame. There may be multiple such paths. To do so, when you have an object selected
     in the tree (e.g. from the steps above), click "GC Root" in the toolbar above.

Additionally, some objects, like Frames may stick around even after being closed. You can prevent this by making them be disposed when closed:
  `setDefaultCloseOperation(DISPOSE_ON_CLOSE);`
