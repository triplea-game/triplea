package games.strategy.triplea.ai.pro.data;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.ai.pro.logging.ProLogger;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;

/**
 * Takes all available purchase options, filters out those which the AI can't handle, and sorts them
 * into categories.
 */
public class ProPurchaseOptionMap {

  @Getter private final List<ProPurchaseOption> landFodderOptions;
  @Getter private final List<ProPurchaseOption> landAttackOptions;
  @Getter private final List<ProPurchaseOption> landDefenseOptions;
  @Getter private final List<ProPurchaseOption> landZeroMoveOptions;
  @Getter private final List<ProPurchaseOption> airOptions;
  @Getter private final List<ProPurchaseOption> seaDefenseOptions;
  @Getter private final List<ProPurchaseOption> seaTransportOptions;
  private final List<ProPurchaseOption> seaCarrierOptions;
  private final List<ProPurchaseOption> seaSubOptions;
  @Getter private final List<ProPurchaseOption> aaOptions;
  @Getter private final List<ProPurchaseOption> factoryOptions;
  private final List<ProPurchaseOption> specialOptions;

  public ProPurchaseOptionMap(final GamePlayer player, final GameData data) {

    ProLogger.info("Purchase Options");

    // Initialize lists
    landFodderOptions = new ArrayList<>();
    landAttackOptions = new ArrayList<>();
    landDefenseOptions = new ArrayList<>();
    landZeroMoveOptions = new ArrayList<>();
    airOptions = new ArrayList<>();
    seaDefenseOptions = new ArrayList<>();
    seaTransportOptions = new ArrayList<>();
    seaCarrierOptions = new ArrayList<>();
    seaSubOptions = new ArrayList<>();
    aaOptions = new ArrayList<>();
    factoryOptions = new ArrayList<>();
    specialOptions = new ArrayList<>();

    // Add each production rule to appropriate list(s)
    final ProductionFrontier productionFrontier = player.getProductionFrontier();
    if (productionFrontier == null || productionFrontier.getRules() == null) {
      return;
    }
    for (final ProductionRule rule : productionFrontier.getRules()) {

      // Check if rule is for a unit
      final NamedAttachable resourceOrUnit = rule.getAnyResultKey();
      if (!(resourceOrUnit instanceof UnitType)) {
        continue;
      }
      final UnitType unitType = (UnitType) resourceOrUnit;

      // Add rule to appropriate purchase option list
      if (unitType.getUnitAttachment().getIsSuicideOnHit()
          || canUnitTypeSuicide(unitType, player)) {
        final ProPurchaseOption ppo = new ProPurchaseOption(rule, unitType, player, data);
        specialOptions.add(ppo);
        ProLogger.debug("Special: " + ppo);
      } else if (Matches.unitTypeCanProduceUnits().test(unitType)
          && Matches.unitTypeIsInfrastructure().test(unitType)) {
        final ProPurchaseOption ppo = new ProPurchaseOption(rule, unitType, player, data);
        factoryOptions.add(ppo);
        ProLogger.debug("Factory: " + ppo);
      } else if (unitType.getUnitAttachment().getMovement(player) <= 0
          && Matches.unitTypeIsLand().test(unitType)) {
        final ProPurchaseOption ppo = new ProPurchaseOption(rule, unitType, player, data);
        landZeroMoveOptions.add(ppo);
        ProLogger.debug("Zero Move Land: " + ppo);
      } else if (Matches.unitTypeIsLand().test(unitType)) {
        final ProPurchaseOption ppo = new ProPurchaseOption(rule, unitType, player, data);
        if (!Matches.unitTypeIsInfrastructure().test(unitType)) {
          landFodderOptions.add(ppo);
        }
        if ((ppo.getAttack() > 0 || ppo.isAttackSupport())
            && (ppo.getAttack() >= ppo.getDefense() || ppo.getMovement() > 1)) {
          landAttackOptions.add(ppo);
        }
        if ((ppo.getDefense() > 0 || ppo.isDefenseSupport())
            && (ppo.getDefense() >= ppo.getAttack() || ppo.getMovement() > 1)) {
          landDefenseOptions.add(ppo);
        }
        if (Matches.unitTypeIsAaForBombingThisUnitOnly().test(unitType)) {
          aaOptions.add(ppo);
        }
        ProLogger.debug("Land: " + ppo);
      } else if (Matches.unitTypeIsAir().test(unitType)) {
        final ProPurchaseOption ppo = new ProPurchaseOption(rule, unitType, player, data);
        airOptions.add(ppo);
        ProLogger.debug("Air: " + ppo);
      } else if (Matches.unitTypeIsSea().test(unitType)) {
        final ProPurchaseOption ppo = new ProPurchaseOption(rule, unitType, player, data);
        if (!ppo.isSub()) {
          seaDefenseOptions.add(ppo);
        }
        if (ppo.isTransport()) {
          seaTransportOptions.add(ppo);
        }
        if (ppo.isCarrier()) {
          seaCarrierOptions.add(ppo);
        }
        if (ppo.isSub()) {
          seaSubOptions.add(ppo);
        }
        ProLogger.debug("Sea: " + ppo);
      }
    }
    if (landAttackOptions.isEmpty()) {
      landAttackOptions.addAll(landDefenseOptions);
    }
    if (landDefenseOptions.isEmpty()) {
      landDefenseOptions.addAll(landAttackOptions);
    }

    // Print categorized options
    ProLogger.info("Purchase Categories");
    logOptions(landFodderOptions, "Land Fodder Options: ");
    logOptions(landAttackOptions, "Land Attack Options: ");
    logOptions(landDefenseOptions, "Land Defense Options: ");
    logOptions(landZeroMoveOptions, "Land Zero Move Options: ");
    logOptions(airOptions, "Air Options: ");
    logOptions(seaDefenseOptions, "Sea Defense Options: ");
    logOptions(seaTransportOptions, "Sea Transport Options: ");
    logOptions(seaCarrierOptions, "Sea Carrier Options: ");
    logOptions(seaSubOptions, "Sea Sub Options: ");
    logOptions(aaOptions, "AA Options: ");
    logOptions(factoryOptions, "Factory Options: ");
    logOptions(specialOptions, "Special Options: ");
  }

  private boolean canUnitTypeSuicide(final UnitType unitType, final GamePlayer player) {
    return (unitType.getUnitAttachment().getIsSuicideOnAttack()
            && unitType.getUnitAttachment().getMovement(player) > 0)
        || unitType.getUnitAttachment().getIsSuicideOnDefense();
  }

  public List<ProPurchaseOption> getAllOptions() {
    final Set<ProPurchaseOption> allOptions = new HashSet<>();
    allOptions.addAll(getLandOptions());
    allOptions.addAll(landZeroMoveOptions);
    allOptions.addAll(airOptions);
    allOptions.addAll(getSeaOptions());
    allOptions.addAll(aaOptions);
    allOptions.addAll(factoryOptions);
    allOptions.addAll(specialOptions);
    return new ArrayList<>(allOptions);
  }

  public List<ProPurchaseOption> getLandOptions() {
    final Set<ProPurchaseOption> landOptions = new HashSet<>();
    landOptions.addAll(landFodderOptions);
    landOptions.addAll(landAttackOptions);
    landOptions.addAll(landDefenseOptions);
    return new ArrayList<>(landOptions);
  }

  private List<ProPurchaseOption> getSeaOptions() {
    final Set<ProPurchaseOption> seaOptions = new HashSet<>();
    seaOptions.addAll(seaDefenseOptions);
    seaOptions.addAll(seaTransportOptions);
    seaOptions.addAll(seaCarrierOptions);
    seaOptions.addAll(seaSubOptions);
    return new ArrayList<>(seaOptions);
  }

  private static void logOptions(final List<ProPurchaseOption> purchaseOptions, final String name) {
    final StringBuilder sb = new StringBuilder(name);
    for (final ProPurchaseOption ppo : purchaseOptions) {
      sb.append(ppo.getUnitType().getName());
      sb.append(", ");
    }
    sb.delete(sb.length() - 2, sb.length());
    ProLogger.debug(sb.toString());
  }
}
