package games.strategy.performance;

import java.io.Closeable;

public class PerfTimer implements Closeable {

  protected static final PerfTimer DISABLED_TIMER = new PerfTimer("disabled");

  public final String title;
  private final long startMillis;

  protected PerfTimer(String title) {
    this.title = title;
    this.startMillis = System.nanoTime();
  }
  private long stopTimer() {
    long end = System.nanoTime();
    return end-startMillis;
  }
  @Override
  public void close() {
    Perf.processResult(stopTimer(), this);
  }
  /** Alias for the close method, stops the timer*/
  public void stop() {
    close();
  }
}
