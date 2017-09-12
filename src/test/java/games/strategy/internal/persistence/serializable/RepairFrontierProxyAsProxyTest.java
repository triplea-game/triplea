package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.newRepairFrontier;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.RepairFrontier;

public final class RepairFrontierProxyAsProxyTest extends AbstractGameDataComponentProxyTestCase<RepairFrontier> {
  public RepairFrontierProxyAsProxyTest() {
    super(RepairFrontier.class);
  }

  @Override
  protected Collection<RepairFrontier> createPrincipals() {
    return Arrays.asList(newRepairFrontier(getGameData(), "repairFrontier"));
  }
}
