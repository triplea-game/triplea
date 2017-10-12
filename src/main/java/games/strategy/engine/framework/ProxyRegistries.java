package games.strategy.engine.framework;

import java.util.Arrays;
import java.util.Collection;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.internal.persistence.serializable.AaRadarAdvanceProxy;
import games.strategy.internal.persistence.serializable.DestroyerBombardTechAdvanceProxy;
import games.strategy.internal.persistence.serializable.GuidProxy;
import games.strategy.internal.persistence.serializable.HeavyBomberAdvanceProxy;
import games.strategy.internal.persistence.serializable.ImprovedArtillerySupportAdvanceProxy;
import games.strategy.internal.persistence.serializable.ImprovedShipyardsAdvanceProxy;
import games.strategy.internal.persistence.serializable.IncreasedFactoryProductionAdvanceProxy;
import games.strategy.internal.persistence.serializable.IndustrialTechnologyAdvanceProxy;
import games.strategy.internal.persistence.serializable.IntegerMapProxy;
import games.strategy.internal.persistence.serializable.JetPowerAdvanceProxy;
import games.strategy.internal.persistence.serializable.LongRangeAircraftAdvanceProxy;
import games.strategy.internal.persistence.serializable.MechanizedInfantryAdvanceProxy;
import games.strategy.internal.persistence.serializable.ParatroopersAdvanceProxy;
import games.strategy.internal.persistence.serializable.PlayerIdProxy;
import games.strategy.internal.persistence.serializable.ProductionFrontierProxy;
import games.strategy.internal.persistence.serializable.ProductionRuleProxy;
import games.strategy.internal.persistence.serializable.PropertyBagMementoProxy;
import games.strategy.internal.persistence.serializable.RepairFrontierProxy;
import games.strategy.internal.persistence.serializable.RepairRuleProxy;
import games.strategy.internal.persistence.serializable.ResourceProxy;
import games.strategy.internal.persistence.serializable.RocketsAdvanceProxy;
import games.strategy.internal.persistence.serializable.SuperSubsAdvanceProxy;
import games.strategy.internal.persistence.serializable.TechnologyFrontierProxy;
import games.strategy.internal.persistence.serializable.TripleAProxy;
import games.strategy.internal.persistence.serializable.UnitProxy;
import games.strategy.internal.persistence.serializable.UnitTypeProxy;
import games.strategy.internal.persistence.serializable.VersionProxy;
import games.strategy.internal.persistence.serializable.WarBondsAdvanceProxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.persistence.serializable.ProxyRegistry;

/**
 * A collection of pre-configured proxy registries.
 */
public final class ProxyRegistries {
  /**
   * A proxy registry that has been configured with all proxy factories required to serialize a game data memento.
   */
  public static final ProxyRegistry GAME_DATA_MEMENTO = ProxyRegistry.newInstance(getGameDataMementoProxyFactories());

  private ProxyRegistries() {}

  @VisibleForTesting
  static Collection<ProxyFactory> getGameDataMementoProxyFactories() {
    return Arrays.asList(
        AaRadarAdvanceProxy.FACTORY,
        DestroyerBombardTechAdvanceProxy.FACTORY,
        GuidProxy.FACTORY,
        HeavyBomberAdvanceProxy.FACTORY,
        ImprovedArtillerySupportAdvanceProxy.FACTORY,
        ImprovedShipyardsAdvanceProxy.FACTORY,
        IncreasedFactoryProductionAdvanceProxy.FACTORY,
        IndustrialTechnologyAdvanceProxy.FACTORY,
        IntegerMapProxy.FACTORY,
        JetPowerAdvanceProxy.FACTORY,
        LongRangeAircraftAdvanceProxy.FACTORY,
        MechanizedInfantryAdvanceProxy.FACTORY,
        ParatroopersAdvanceProxy.FACTORY,
        PlayerIdProxy.FACTORY,
        ProductionFrontierProxy.FACTORY,
        ProductionRuleProxy.FACTORY,
        PropertyBagMementoProxy.FACTORY,
        RepairFrontierProxy.FACTORY,
        RepairRuleProxy.FACTORY,
        ResourceProxy.FACTORY,
        RocketsAdvanceProxy.FACTORY,
        SuperSubsAdvanceProxy.FACTORY,
        TechnologyFrontierProxy.FACTORY,
        TripleAProxy.FACTORY,
        UnitProxy.FACTORY,
        UnitTypeProxy.FACTORY,
        VersionProxy.FACTORY,
        WarBondsAdvanceProxy.FACTORY);
  }
}
