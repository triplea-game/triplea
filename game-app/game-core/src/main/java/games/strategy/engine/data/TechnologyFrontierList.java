package games.strategy.engine.data;

import games.strategy.triplea.delegate.TechAdvance;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NonNls;

/** A collection of {@link TechnologyFrontier}s. */
public class TechnologyFrontierList extends GameDataComponent {
  @Serial private static final long serialVersionUID = 2958122401265284935L;

  private final List<TechnologyFrontier> technologyFrontiers = new ArrayList<>();

  public TechnologyFrontierList(final GameData data) {
    super(data);
  }

  public void addTechnologyFrontier(final TechnologyFrontier tf) {
    technologyFrontiers.add(tf);
  }

  public int size() {
    return technologyFrontiers.size();
  }

  public static TechnologyFrontier getTechnologyFrontierOrThrow(
      final @Nonnull GamePlayer player, final @NonNls String name) {
    for (final TechnologyFrontier tf : player.getTechnologyFrontierList().technologyFrontiers) {
      if (tf.getName().equals(name)) {
        return tf;
      }
    }
    throw new IllegalStateException(
        "TechnologyFrontier doesn't exist: " + name + " for player: " + player);
  }

  public List<TechAdvance> getAdvances() {
    final List<TechAdvance> techs = new ArrayList<>();
    for (final TechnologyFrontier t : technologyFrontiers) {
      techs.addAll(t.getTechs());
    }
    return Collections.unmodifiableList(techs);
  }

  public List<TechnologyFrontier> getFrontiers() {
    return Collections.unmodifiableList(technologyFrontiers);
  }
}
