# Profiling TripleA

When investigating performance issues or looking for opportunities to improve TripleA performance (for example,
to speed up AI turns), it's useful to profile TripleA execution.

One tool that can be used for this is VisualVM. Here's how it can be used:

  1. Start by launching TripleA (either a release version or your own local build).
  2. If measuring AI performance, save a game at the end of a human turn, right before one or more AI turns.
  3. Before ending the turn, connect to the TripleA process from VisualVM.
  4. Once attached, switch to the Sampler tab and click "CPU".
  5. Switch back to TripleA and end the turn. The AIs will now execute.
  6. Once TripleA is finished the AI turn(s) and it's at the start of a human turn, switch back to VisualVM and click
  the "CPU" button again to stop sampling.

At this point, in the CPU samples view, you should be able to see different threads. There will be one thread that is
the main game thread (e.g. "Thread 6") and a number of worker threads for battle simulation ("ConcurrentOddsCalculator"
threads). You can drill down into these to see where the game is spending a lot of time.

![VisualVM screenshot](https://user-images.githubusercontent.com/17648/80159252-7dc7f100-8598-11ea-95d7-aa5498887995.png)

When you make changes to the engine, you can test them again using the above steps, with the same save game file
as before.

Some functions will appear multiple times in different call stacks. To see the overall resource use of a function, you
can right click on it and click "Find in Hot Spots". The function will be highlighted in the bottom panel and you can
see its total CPU time, across all call sites and threads. Be aware that a function that's called during battle
simulation may have its total CPU time be significantly longer than its contribution to overall runtime, because
battle simulation gets called in parallel by multiple threads.
