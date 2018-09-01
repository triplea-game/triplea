package games.strategy.triplea.delegate.dataObjects;

import java.io.Serializable;
import java.util.List;

public class TechResults implements Serializable {
  private static final long serialVersionUID = 5574673305892105782L;
  private final int[] m_rolls;
  private final int m_hits;
  private final int m_remainder;
  private final List<String> m_advances;
  private final String m_errorString;

  public TechResults(final String errorString) {
    m_errorString = errorString;
    m_remainder = 0;
    m_advances = null;
    m_hits = 0;
    m_rolls = null;
  }

  /**
   * Creates a new TechResults.
   *
   * @param rolls rolls
   * @param remainder remainder
   * @param hits number of hits
   * @param advances a List of Strings
   */
  public TechResults(final int[] rolls, final int remainder, final int hits, final List<String> advances) {
    m_rolls = rolls;
    m_remainder = remainder;
    m_hits = hits;
    m_advances = advances;
    m_errorString = null;
  }

  /**
   * Indicates whether there was an error.
   */
  public boolean isError() {
    return m_errorString != null;
  }

  /**
   * Returns string error or null if no error occurred (use isError to see if there was an error).
   */
  public String getErrorString() {
    return m_errorString;
  }

  public int getHits() {
    return m_hits;
  }

  public int getRemainder() {
    return m_remainder;
  }

  public int[] getRolls() {
    return m_rolls;
  }

  public List<String> getAdvances() {
    return m_advances;
  }
}
