package games.strategy.triplea.ai.pro.data;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.pro.logging.ProLogger;
import games.strategy.triplea.ai.pro.util.ProPurchaseUtils;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.power.calculator.SupportCalculator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/** The result of an AI purchase analysis for a single production rule and unit type. */
public class ProPurchaseOption {

  @Getter private final ProductionRule productionRule;
  @Getter private final UnitType unitType;
  private final GamePlayer player;
  @Getter private final int cost;
  @Getter private final IntegerMap<Resource> costs;
  @Getter private final boolean isConstruction;
  @Getter private final String constructionType;
  @Getter private final int constructionTypePerTurn;
  @Getter private final int maxConstructionType;
  @Getter private final int movement;
  @Getter private final int quantity;
  @Getter private int hitPoints;
  @Getter private final double attack;
  private final double amphibAttack;
  @Getter private final double defense;
  @Getter private final int transportCost;
  @Getter private final int carrierCost;
  @Getter private final boolean isAir;
  @Getter private final boolean isSub;
  @Getter private final boolean isDestroyer;
  @Getter private final boolean isTransport;
  @Getter private final boolean isLandTransport;
  @Getter private final boolean isCarrier;
  @Getter private final int carrierCapacity;
  @Getter private final double transportEfficiency;
  @Getter private final double costPerHitPoint;
  private final double hitPointEfficiency;
  @Getter private final double attackEfficiency;
  @Getter private final double defenseEfficiency;
  @Getter private final int maxBuiltPerPlayer;
  private final Set<UnitSupportAttachment> unitSupportAttachments;
  @Getter private boolean isAttackSupport;
  @Getter private boolean isDefenseSupport;
  @Getter private final boolean consumesUnits;

  ProPurchaseOption(
      final ProductionRule productionRule,
      final UnitType unitType,
      final GamePlayer player,
      final GameData data) {
    this.productionRule = productionRule;
    this.unitType = unitType;
    this.player = player;
    final UnitAttachment unitAttachment = unitType.getUnitAttachment();
    final Resource pus = data.getResourceList().getResource(Constants.PUS);
    cost = productionRule.getCosts().getInt(pus);
    costs = productionRule.getCosts();
    isConstruction = unitAttachment.isConstruction();
    constructionType = unitAttachment.getConstructionType();
    constructionTypePerTurn = unitAttachment.getConstructionsPerTerrPerTypePerTurn();
    maxConstructionType = unitAttachment.getMaxConstructionsPerTypePerTerr();
    movement = unitAttachment.getMovement(player);
    quantity = productionRule.getResults().totalValues();
    final boolean isInfra = unitAttachment.isInfrastructure();
    hitPoints = unitAttachment.getHitPoints() * quantity;
    if (isInfra) {
      hitPoints = 0;
    }
    attack = (double) unitAttachment.getAttack(player) * quantity;
    amphibAttack = attack + 0.5 * unitAttachment.getIsMarine() * quantity;
    defense = (double) unitAttachment.getDefense(player) * quantity;
    transportCost = unitAttachment.getTransportCost() * quantity;
    carrierCost = unitAttachment.getCarrierCost() * quantity;
    isAir = unitAttachment.isAir();
    isSub = unitAttachment.getCanEvade();
    isDestroyer = unitAttachment.isDestroyer();
    isTransport = unitAttachment.getTransportCapacity() > 0;
    isLandTransport = unitAttachment.isLandTransport();
    isCarrier = unitAttachment.getCarrierCapacity() > 0;
    carrierCapacity = unitAttachment.getCarrierCapacity() * quantity;
    transportEfficiency = (double) unitAttachment.getTransportCapacity() / cost;
    if (hitPoints == 0) {
      costPerHitPoint = Double.POSITIVE_INFINITY;
    } else {
      costPerHitPoint = ((double) cost) / hitPoints;
    }
    hitPointEfficiency =
        (hitPoints
                + 0.2 * attack * 6 / data.getDiceSides()
                + 0.2 * defense * 6 / data.getDiceSides())
            / cost;
    attackEfficiency =
        (1 + hitPoints)
            * (hitPoints
                + attack * 6 / data.getDiceSides()
                + 0.5 * defense * 6 / data.getDiceSides())
            / cost;
    defenseEfficiency =
        (1 + hitPoints)
            * (hitPoints
                + 0.5 * attack * 6 / data.getDiceSides()
                + defense * 6 / data.getDiceSides())
            / cost;
    maxBuiltPerPlayer = unitAttachment.getMaxBuiltPerPlayer();

    // Support fields
    unitSupportAttachments = UnitSupportAttachment.get(unitType);
    isAttackSupport = false;
    isDefenseSupport = false;
    for (final UnitSupportAttachment usa : unitSupportAttachments) {
      if (usa.getOffence()) {
        isAttackSupport = true;
      }
      if (usa.getDefence()) {
        isDefenseSupport = true;
      }
    }
    consumesUnits = !unitAttachment.getConsumesUnits().isEmpty();
  }

  @Override
  public String toString() {
    return productionRule
        + " | cost="
        + cost
        + " | moves="
        + movement
        + " | quantity="
        + quantity
        + " | hitPointEfficiency="
        + String.format("%.3f", hitPointEfficiency)
        + " | attackEfficiency="
        + String.format("%.3f", attackEfficiency)
        + " | defenseEfficiency="
        + String.format("%.3f", defenseEfficiency)
        + " | isSub="
        + isSub
        + " | isTransport="
        + isTransport
        + " | isCarrier="
        + isCarrier;
  }

  public double getTransportEfficiencyRatio() {
    return Math.pow(transportEfficiency, 30) / quantity;
  }

  public double getFodderEfficiency(
      final int enemyDistance,
      final GameData data,
      final List<Unit> ownedLocalUnits,
      final List<Unit> unitsToPlace) {
    final double supportAttackFactor =
        calculateSupportFactor(ownedLocalUnits, unitsToPlace, data, false);
    final double supportDefenseFactor =
        calculateSupportFactor(ownedLocalUnits, unitsToPlace, data, true);
    final double distanceFactor = Math.sqrt(calculateLandDistanceFactor(enemyDistance));
    return calculateEfficiency(
        0.25, 0.25, supportAttackFactor, supportDefenseFactor, distanceFactor, data);
  }

  public double getAttackEfficiency(
      final int enemyDistance,
      final GameData data,
      final List<Unit> ownedLocalUnits,
      final List<Unit> unitsToPlace) {
    final double supportAttackFactor =
        calculateSupportFactor(ownedLocalUnits, unitsToPlace, data, false);
    final double supportDefenseFactor =
        calculateSupportFactor(ownedLocalUnits, unitsToPlace, data, true);
    final double distanceFactor = calculateLandDistanceFactor(enemyDistance);
    return calculateEfficiency(
        1.25, 0.75, supportAttackFactor, supportDefenseFactor, distanceFactor, data);
  }

  public double getDefenseEfficiency(
      final int enemyDistance,
      final GameData data,
      final List<Unit> ownedLocalUnits,
      final List<Unit> unitsToPlace) {
    final double supportAttackFactor =
        calculateSupportFactor(ownedLocalUnits, unitsToPlace, data, false);
    final double supportDefenseFactor =
        calculateSupportFactor(ownedLocalUnits, unitsToPlace, data, true);
    final double distanceFactor = calculateLandDistanceFactor(enemyDistance);
    return calculateEfficiency(
        0.75, 1.25, supportAttackFactor, supportDefenseFactor, distanceFactor, data);
  }

  /**
   * Returns the sea defense efficiency for the specified units if this purchase option is selected.
   */
  public double getSeaDefenseEfficiency(
      final GameData data,
      final List<Unit> ownedLocalUnits,
      final List<Unit> unitsToPlace,
      final boolean needDestroyer,
      final int unusedCarrierCapacity,
      final int unusedLocalCarrierCapacity) {
    if (isAir
        && (carrierCost <= 0
            || carrierCost > unusedCarrierCapacity
            || !Properties.getProduceFightersOnCarriers(data.getProperties()))) {
      return 0;
    }
    final double supportAttackFactor =
        calculateSupportFactor(ownedLocalUnits, unitsToPlace, data, false);
    final double supportDefenseFactor =
        calculateSupportFactor(ownedLocalUnits, unitsToPlace, data, true);
    double seaFactor = 1;
    if (needDestroyer && isDestroyer) {
      seaFactor = 8;
    }
    if (isAir || (carrierCapacity > 0 && unusedLocalCarrierCapacity <= 0)) {
      seaFactor = 4;
    }
    return calculateEfficiency(
        0.75, 1, supportAttackFactor, supportDefenseFactor, movement, seaFactor, data);
  }

  /** Calculates amphibious assault efficiency coefficient. */
  public double getAmphibEfficiency(
      final GameData data, final List<Unit> ownedLocalUnits, final List<Unit> unitsToPlace) {
    final double supportAttackFactor =
        calculateSupportFactor(ownedLocalUnits, unitsToPlace, data, false);
    final double supportDefenseFactor =
        calculateSupportFactor(ownedLocalUnits, unitsToPlace, data, true);
    final double hitPointPerUnitFactor = (3 + (double) hitPoints / quantity);
    final double transportCostFactor = Math.pow(1.0 / transportCost, .2);
    final double attackValue =
        (amphibAttack + supportAttackFactor * quantity) * 6 / data.getDiceSides();
    final double defenseValue =
        (defense + supportDefenseFactor * quantity) * 6 / data.getDiceSides();
    return Math.pow(
            ((2 * hitPoints) + attackValue + defenseValue)
                * hitPointPerUnitFactor
                * transportCostFactor
                / cost,
            30)
        / quantity;
  }

  private double calculateLandDistanceFactor(final int enemyDistance) {
    if (movement <= 0) {
      return 0.1; // Set 0 move units to an order of magnitude less than 1 move units
    }
    final double distance = Math.max(0, enemyDistance - 1.5);
    final int moveValue = isLandTransport ? (movement + 1) : movement;
    // 1, 2, 2.5, 2.75, etc
    final double moveFactor =
        1.0 + 2.0 * (Math.pow(2, moveValue - 1.0) - 1.0) / Math.pow(2, moveValue - 1.0);
    return Math.pow(moveFactor, distance / 5);
  }

  // TODO: doesn't consider enemy support
  private double calculateSupportFactor(
      final List<Unit> ownedLocalUnits,
      final List<Unit> unitsToPlace,
      final GameData data,
      final boolean defense) {
    if ((!isAttackSupport && !defense) || (!isDefenseSupport && defense)) {
      return 0;
    }

    final List<Unit> units = new ArrayList<>();
    units.addAll(unitsToPlace);
    units.addAll(unitType.createTemp(1, player));
    // Omit units that will be consumed by placing units here.
    Collection<Unit> toConsume = ProPurchaseUtils.getUnitsToConsume(player, ownedLocalUnits, units);
    units.addAll(CollectionUtils.difference(ownedLocalUnits, toConsume));

    final SupportCalculator availableSupports =
        new SupportCalculator(
            units,
            data.getUnitTypeList().getSupportRules(),
            defense ? BattleState.Side.DEFENSE : BattleState.Side.OFFENSE,
            true);

    double totalSupportFactor = 0;
    for (final UnitSupportAttachment usa : unitSupportAttachments) {
      for (final List<UnitSupportAttachment> bonusType :
          availableSupports.getUnitSupportAttachments()) {
        if (!bonusType.contains(usa)) {
          continue;
        }

        // Find number of support provided and supportable units
        int numAddedSupport = usa.getNumber();
        if (usa.getImpArtTech() && TechTracker.hasImprovedArtillerySupport(player)) {
          numAddedSupport *= 2;
        }
        int numSupportProvided = -numAddedSupport;
        final Set<Unit> supportableUnits = new HashSet<>();
        for (final UnitSupportAttachment usa2 : bonusType) {
          numSupportProvided += availableSupports.getSupport(usa2);
          supportableUnits.addAll(
              CollectionUtils.getMatches(units, Matches.unitIsOfTypes(usa2.getUnitType())));
        }
        final int numSupportableUnits = supportableUnits.size();

        // Find ratio of supportable to support units (optimal 2 to 1)
        final int numExtraSupportableUnits = Math.max(0, numSupportableUnits - numSupportProvided);

        // Ranges from 0 to 1
        final double ratio =
            Math.min(1, 2.0 * numExtraSupportableUnits / (numSupportableUnits + numAddedSupport));

        // Find approximate strength bonus provided
        double bonus = 0;
        if (usa.getStrength()) {
          bonus += usa.getBonus();
        }
        if (usa.getRoll()) {
          bonus += (usa.getBonus() * data.getDiceSides() * 0.75);
        }

        // Find support factor value
        final double supportFactor = Math.pow(numAddedSupport * 0.9, 0.9) * bonus * ratio;
        totalSupportFactor += supportFactor;
        ProLogger.trace(
            unitType.getName()
                + ", bonusType="
                + usa.getBonusType()
                + ", supportFactor="
                + supportFactor
                + ", numSupportProvided="
                + numSupportProvided
                + ", numSupportableUnits="
                + numSupportableUnits
                + ", numAddedSupport="
                + numAddedSupport
                + ", ratio="
                + ratio
                + ", bonus="
                + bonus);
      }
    }
    ProLogger.debug(
        unitType.getName() + ", defense=" + defense + ", totalSupportFactor=" + totalSupportFactor);
    return totalSupportFactor;
  }

  private double calculateEfficiency(
      final double attackFactor,
      final double defenseFactor,
      final double supportAttackFactor,
      final double supportDefenseFactor,
      final double distanceFactor,
      final GameData data) {
    return calculateEfficiency(
        attackFactor,
        defenseFactor,
        supportAttackFactor,
        supportDefenseFactor,
        distanceFactor,
        1,
        data);
  }

  private double calculateEfficiency(
      final double attackFactor,
      final double defenseFactor,
      final double supportAttackFactor,
      final double supportDefenseFactor,
      final double distanceFactor,
      final double seaFactor,
      final GameData data) {
    final double hitPointPerUnitFactor = (3 + (double) hitPoints / quantity);
    final double attackValue =
        attackFactor * (attack + supportAttackFactor * quantity) * 6 / data.getDiceSides();
    final double defenseValue =
        defenseFactor * (defense + supportDefenseFactor * quantity) * 6 / data.getDiceSides();
    return Math.pow(
            ((2 * hitPoints) + attackValue + defenseValue)
                * hitPointPerUnitFactor
                * distanceFactor
                * seaFactor
                / cost,
            30)
        / quantity;
  }

  public List<Unit> createTempUnits() {
    return getUnitType().createTemp(getQuantity(), player);
  }
}
