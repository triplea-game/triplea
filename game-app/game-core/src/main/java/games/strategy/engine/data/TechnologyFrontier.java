package games.strategy.engine.data;

import games.strategy.triplea.delegate.TechAdvance;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/** A collection of {@link TechAdvance}s available to a single player. */
public class TechnologyFrontier extends GameDataComponent implements Iterable<TechAdvance> {
  private static final long serialVersionUID = -5245743727479551766L;

  private final List<TechAdvance> techs = new ArrayList<>();
  private List<TechAdvance> cachedTechs;
  private final String name;

  public TechnologyFrontier(final String name, final GameData data) {
    super(data);
    this.name = name;
  }

  public TechnologyFrontier(final TechnologyFrontier other) {
    super(other.getData());
    name = other.name;
    techs.addAll(other.techs);
  }

  private void reorderTechsToMatchGameTechsOrder() {
    final GameData gameData = getData();
    if (gameData != null) {
      techs.sort(Comparator.comparingInt(gameData.getTechnologyFrontier().getTechs()::indexOf));
      cachedTechs = null;
    }
  }

  public void addAdvance(final TechAdvance t) {
    cachedTechs = null;
    techs.add(t);
    reorderTechsToMatchGameTechsOrder();
  }

  public void addAdvance(final List<TechAdvance> list) {
    for (final TechAdvance t : list) {
      addAdvance(t);
    }
  }

  public void removeAdvance(final TechAdvance t) {
    if (!techs.contains(t)) {
      throw new IllegalStateException("Advance not present:" + t);
    }
    cachedTechs = null;
    techs.remove(t);
  }

  public TechAdvance getAdvanceByProperty(final String property) {
    return techs.stream().filter(ta -> ta.getProperty().equals(property)).findAny().orElse(null);
  }

  public TechAdvance getAdvanceByName(final String name) {
    return techs.stream().filter(ta -> ta.getName().equals(name)).findAny().orElse(null);
  }

  public List<TechAdvance> getTechs() {
    if (cachedTechs == null) {
      cachedTechs = Collections.unmodifiableList(techs);
    }
    return cachedTechs;
  }

  @Override
  public Iterator<TechAdvance> iterator() {
    return getTechs().iterator();
  }

  public String getName() {
    return name;
  }

  public boolean isEmpty() {
    return techs.isEmpty();
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TechnologyFrontier)) {
      return false;
    }
    final TechnologyFrontier other = (TechnologyFrontier) o;
    return name.equals(other.getName());
  }
}
