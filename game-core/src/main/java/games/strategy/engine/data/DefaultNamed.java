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
    if ((name == null) || (name.length() == 0)) {
      throw new IllegalArgumentException("Name must not be null");
    }
    m_name = name;
  }

  @Override
  public String getName() {
    return m_name;
  }

  @Override
  public boolean equals(final Object o) {
    if ((o == null) || !(o instanceof Named)) {
      return false;
    }
    final Named other = (Named) o;
    return this.m_name.equals(other.getName());
  }

  @Override
  public int hashCode() {
    if (m_name == null) {
      logSerializationWarning();
    }
    return Objects.hashCode(m_name);
  }

  private static void logSerializationWarning() {
    ClientLogger.logQuietly("Warning: serialization de-serializatoin error, m_name in DefaultNamed.java is null.");
  }

  @Override
  public String toString() {
    if (m_name == null) {
      logSerializationWarning();
    }
    return MoreObjects.toStringHelper(getClass()).add("name", m_name).toString();
  }
}
