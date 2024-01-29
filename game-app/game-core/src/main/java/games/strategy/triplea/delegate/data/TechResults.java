package games.strategy.triplea.delegate.data;

import java.io.Serializable;
import java.util.List;
import lombok.Getter;

/** The result of spending tech tokens. */
@Getter
public class TechResults implements Serializable {
  private static final long serialVersionUID = 5574673305892105782L;
  private final int[] rolls;
  private final int hits;
  private final int remainder;
  private final List<String> advances;

  /**
   * -- GETTER -- Returns string error or null if no error occurred (use isError to see if there was
   * an error).
   */
  private final String errorString;

  public TechResults(final String errorString) {
    this.errorString = errorString;
    remainder = 0;
    advances = null;
    hits = 0;
    rolls = null;
  }

  public TechResults(
      final int[] rolls, final int remainder, final int hits, final List<String> advances) {
    this.rolls = rolls;
    this.remainder = remainder;
    this.hits = hits;
    this.advances = advances;
    errorString = null;
  }

  /** Indicates whether there was an error. */
  public boolean isError() {
    return errorString != null;
  }
}
