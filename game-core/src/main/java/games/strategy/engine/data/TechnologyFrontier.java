package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import games.strategy.triplea.delegate.TechAdvance;

public class TechnologyFrontier extends GameDataComponent implements Iterable<TechAdvance> {
  private static final long serialVersionUID = -5245743727479551766L;
  private final List<TechAdvance> m_techs = new ArrayList<>();
  private List<TechAdvance> m_cachedTechs;
  private final String m_name;

  public TechnologyFrontier(final String name, final GameData data) {
    super(data);
    m_name = name;
  }

  public TechnologyFrontier(final TechnologyFrontier other) {
    super(other.getData());
    m_name = other.m_name;
    m_techs.addAll(other.m_techs);
  }

  private void reorderTechsToMatchGameTechsOrder() {
    final GameData gameData = getData();
    if (gameData != null) {
      m_techs.sort(Comparator.comparing(gameData.getTechnologyFrontier().getTechs()::indexOf));
      m_cachedTechs = null;
    }
  }

  public void addAdvance(final TechAdvance t) {
    m_cachedTechs = null;
    m_techs.add(t);
    reorderTechsToMatchGameTechsOrder();
  }

  public void addAdvance(final List<TechAdvance> list) {
    for (final TechAdvance t : list) {
      addAdvance(t);
    }
  }

  public void removeAdvance(final TechAdvance t) {
    if (!m_techs.contains(t)) {
      throw new IllegalStateException("Advance not present:" + t);
    }
    m_cachedTechs = null;
    m_techs.remove(t);
  }

  public TechAdvance getAdvanceByProperty(final String property) {
    for (final TechAdvance ta : m_techs) {
      if (ta.getProperty().equals(property)) {
        return ta;
      }
    }
    return null;
  }

  public TechAdvance getAdvanceByName(final String name) {
    for (final TechAdvance ta : m_techs) {
      if (ta.getName().equals(name)) {
        return ta;
      }
    }
    return null;
  }

  public List<TechAdvance> getTechs() {
    if (m_cachedTechs == null) {
      m_cachedTechs = Collections.unmodifiableList(m_techs);
    }
    return m_cachedTechs;
  }

  @Override
  public Iterator<TechAdvance> iterator() {
    return getTechs().iterator();
  }

  public String getName() {
    return m_name;
  }

  public boolean isEmpty() {
    return m_techs.isEmpty();
  }

  @Override
  public String toString() {
    return m_name;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(m_name);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if ((o == null) || !(o instanceof TechnologyFrontier)) {
      return false;
    }
    final TechnologyFrontier other = (TechnologyFrontier) o;
    return m_name.equals(other.getName());
  }
}
