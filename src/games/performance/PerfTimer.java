package games.performance;

public class PerfTimer {

  protected static final PerfTimer DISABLED_TIMER = new PerfTimer("disabled");

  public final String title;
  private final long startMillis;

  public PerfTimer(String title) {
    this.title = title;
    this.startMillis = System.currentTimeMillis();
  }
  protected long stop() {
    long end = System.currentTimeMillis();
    return end-startMillis;
  }
}
