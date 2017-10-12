package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.newTechnologyFrontier;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.TechnologyFrontier;

public final class TechnologyFrontierProxyAsProxyTest
    extends AbstractGameDataComponentProxyTestCase<TechnologyFrontier> {
  public TechnologyFrontierProxyAsProxyTest() {
    super(TechnologyFrontier.class);
  }

  @Override
  protected Collection<TechnologyFrontier> createPrincipals() {
    return Arrays.asList(newTechnologyFrontier(getGameData(), "technologyFrontier"));
  }
}
