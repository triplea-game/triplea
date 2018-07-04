package games.strategy.engine.data;

import java.util.Objects;

import com.google.common.base.MoreObjects;

import games.strategy.debug.ClientLogger;

public class DefaultNamed extends GameDataComponent implements Named {
  private static final long serialVersionUID = -5737716450699952621L;
  private final String m_name;

  /** Creates new DefaultNamed. */
  public DefaultNamed(final String name, final GameData data) {
    super(data);
    if (name == null || name.length() == 0) {
      throw new IllegalArgumentException("Name must not be null");
    }
    m_name = name;
  }

  @Override
  public String getName() {
    return m_name;
  }

  private static String capitalizeFirst(final String s) {
    return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  /**
   * Returns a capitalized name.
   *
   * @return A capitalized name.
   */
  public String getNameCapitalized() {
    return capitalizeFirst(m_name);
  }

  @Override
  public boolean equals(final Object other) {
    if (other instanceof Named) {
      return Objects.equals(m_name, ((Named) other).getName());
    }
    return false;
  }

  @Override
  public int hashCode() {
    if (m_name == null) {
      logSerializationWarning();
    }
    return Objects.hashCode(m_name);
  }

  private static void logSerializationWarning() {
    ClientLogger.logQuietly("Warning: serialization de-serialization error, m_name in DefaultNamed.java is null.");
  }

  @Override
  public String toString() {
    if (m_name == null) {
      logSerializationWarning();
    }
    return MoreObjects.toStringHelper(getClass()).add("name", m_name).toString();
  }
}
