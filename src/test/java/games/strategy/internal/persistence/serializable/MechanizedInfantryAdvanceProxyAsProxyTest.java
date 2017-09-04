package games.strategy.internal.persistence.serializable;

import games.strategy.engine.data.EngineDataEqualityComparators;
import games.strategy.triplea.delegate.MechanizedInfantryAdvance;

public final class MechanizedInfantryAdvanceProxyAsProxyTest
    extends AbstractTechAdvanceProxyTestCase<MechanizedInfantryAdvance> {
  public MechanizedInfantryAdvanceProxyAsProxyTest() {
    super(
        MechanizedInfantryAdvance.class,
        MechanizedInfantryAdvance::new,
        EngineDataEqualityComparators.MECHANIZED_INFANTRY_ADVANCE,
        MechanizedInfantryAdvanceProxy.FACTORY);
  }
}
