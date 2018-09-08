package games.strategy.engine.data;

import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 * Default implementation of {@link Named} for game data components.
 */
public class DefaultNamed extends GameDataComponent implements Named {
  private static final long serialVersionUID = -5737716450699952621L;
  private final String m_name;

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

  @Override
  public boolean equals(final Object other) {
    if (other instanceof Named) {
      return Objects.equals(m_name, ((Named) other).getName());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(m_name);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(getClass()).add("name", m_name).toString();
  }
}
