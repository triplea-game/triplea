package games.strategy.internal.persistence.serializable;

import games.strategy.triplea.delegate.MechanizedInfantryAdvance;

public final class MechanizedInfantryAdvanceProxyAsProxyTest
    extends AbstractTechAdvanceProxyTestCase<MechanizedInfantryAdvance> {
  public MechanizedInfantryAdvanceProxyAsProxyTest() {
    super(MechanizedInfantryAdvance.class, MechanizedInfantryAdvance::new);
  }
}
