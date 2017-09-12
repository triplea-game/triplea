package games.strategy.internal.persistence.serializable;

import static games.strategy.engine.data.TestGameDataComponentFactory.newRepairRule;

import java.util.Arrays;
import java.util.Collection;

import games.strategy.engine.data.RepairRule;

public final class RepairRuleProxyAsProxyTest extends AbstractGameDataComponentProxyTestCase<RepairRule> {
  public RepairRuleProxyAsProxyTest() {
    super(RepairRule.class);
  }

  @Override
  protected Collection<RepairRule> createPrincipals() {
    return Arrays.asList(newRepairRule(getGameData(), "repairRule"));
  }
}
