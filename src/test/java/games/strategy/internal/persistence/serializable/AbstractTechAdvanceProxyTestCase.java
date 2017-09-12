package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.engine.data.TestGameDataComponentFactory.initializeAttachable;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.delegate.TechAdvance;

/**
 * A fixture for testing the basic aspects of {@link TechAdvance} proxy classes.
 *
 * @param <T> The type of the technology advance to be proxied.
 */
public abstract class AbstractTechAdvanceProxyTestCase<T extends TechAdvance>
    extends AbstractGameDataComponentProxyTestCase<T> {
  private final Function<GameData, T> newTechAdvance;

  protected AbstractTechAdvanceProxyTestCase(final Class<T> principalType, final Function<GameData, T> newTechAdvance) {
    super(principalType);

    checkNotNull(newTechAdvance);

    this.newTechAdvance = newTechAdvance;
  }

  @Override
  protected final Collection<T> createPrincipals() {
    final T techAdvance = newTechAdvance.apply(getGameData());
    initializeAttachable(techAdvance);
    return Arrays.asList(techAdvance);
  }
}
