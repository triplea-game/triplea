package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedUnitHolder;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitCollection;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;

/**
 * A serializable proxy for the {@link UnitCollection} class.
 */
@Immutable
public final class UnitCollectionProxy implements Proxy {
  private static final long serialVersionUID = 3937405630115769153L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(UnitCollection.class, UnitCollectionProxy::new);

  private final GameData gameData;
  private final NamedUnitHolder namedUnitHolder;
  private final Collection<Unit> units;

  public UnitCollectionProxy(final UnitCollection unitCollection) {
    checkNotNull(unitCollection);

    gameData = unitCollection.getData();
    namedUnitHolder = unitCollection.getHolder();
    units = unitCollection.getUnits();
  }

  @Override
  public Object readResolve() {
    final UnitCollection unitCollection = new UnitCollection(namedUnitHolder, gameData);
    unitCollection.addAll(units);
    return unitCollection;
  }
}
