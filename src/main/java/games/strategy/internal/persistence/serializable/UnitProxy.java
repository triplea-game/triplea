package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.net.GUID;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;

/**
 * A serializable proxy for the {@link Unit} class.
 *
 * <p>
 * This proxy does not serialize the unit owner to avoid a circular reference. Instances of {@link Unit} created from
 * this proxy will always have their owner set to {@link PlayerID#NULL_PLAYERID}. Proxies that may compose instances of
 * this proxy are required to manually restore the unit owner in their {@code readResolve()} method via a
 * context-dependent mechanism.
 * </p>
 */
@Immutable
public final class UnitProxy implements Proxy {
  private static final long serialVersionUID = 1949624850507306628L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(Unit.class, UnitProxy::new);

  private final GameData gameData;
  private final int hits;
  private final GUID id;
  private final UnitType type;

  public UnitProxy(final Unit unit) {
    checkNotNull(unit);

    gameData = unit.getData();
    hits = unit.getHits();
    id = unit.getID();
    type = unit.getType();
  }

  @Override
  public Object readResolve() {
    final Unit unit = new Unit(type, PlayerID.NULL_PLAYERID, gameData, id);
    unit.setHits(hits);
    return unit;
  }
}
