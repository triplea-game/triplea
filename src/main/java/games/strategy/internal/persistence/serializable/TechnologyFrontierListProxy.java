package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.TechnologyFrontierList;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;

/**
 * A serializable proxy for the {@link TechnologyFrontierList} class.
 */
@Immutable
public final class TechnologyFrontierListProxy implements Proxy {
  private static final long serialVersionUID = -6058055350865404356L;

  public static final ProxyFactory FACTORY =
      ProxyFactory.newInstance(TechnologyFrontierList.class, TechnologyFrontierListProxy::new);

  private final GameData gameData;
  private final List<TechnologyFrontier> technologyFrontiers;

  public TechnologyFrontierListProxy(final TechnologyFrontierList technologyFrontierList) {
    checkNotNull(technologyFrontierList);

    gameData = technologyFrontierList.getData();
    technologyFrontiers = technologyFrontierList.getFrontiers();
  }

  @Override
  public Object readResolve() {
    final TechnologyFrontierList technologyFrontierList = new TechnologyFrontierList(gameData);
    technologyFrontiers.forEach(technologyFrontierList::addTechnologyFrontier);
    return technologyFrontierList;
  }
}
