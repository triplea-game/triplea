package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.engine.data.TestGameDataComponentFactory.initializeAttachable;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

import games.strategy.engine.data.GameData;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.test.EqualityComparator;
import games.strategy.triplea.delegate.TechAdvance;

/**
 * A fixture for testing the basic aspects of {@link TechAdvance} proxy classes.
 *
 * @param <T> The type of the technology advance to be proxied.
 */
public abstract class AbstractTechAdvanceProxyTestCase<T extends TechAdvance>
    extends AbstractGameDataComponentProxyTestCase<T> {
  private final EqualityComparator equalityComparator;
  private final Function<GameData, T> newTechAdvance;
  private final ProxyFactory proxyFactory;

  protected AbstractTechAdvanceProxyTestCase(
      final Class<T> principalType,
      final Function<GameData, T> newTechAdvance,
      final EqualityComparator equalityComparator,
      final ProxyFactory proxyFactory) {
    super(principalType);

    checkNotNull(newTechAdvance);
    checkNotNull(equalityComparator);
    checkNotNull(proxyFactory);

    this.equalityComparator = equalityComparator;
    this.newTechAdvance = newTechAdvance;
    this.proxyFactory = proxyFactory;
  }

  @Override
  protected final T newGameDataComponent(final GameData gameData) {
    final T techAdvance = newTechAdvance.apply(gameData);
    initializeAttachable(techAdvance);
    return techAdvance;
  }

  @Override
  protected final Collection<EqualityComparator> getAdditionalEqualityComparators() {
    return Collections.singletonList(equalityComparator);
  }

  @Override
  protected final Collection<ProxyFactory> getAdditionalProxyFactories() {
    return Collections.singletonList(proxyFactory);
  }
}
