package games.strategy.triplea.delegate;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.TechAttachment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.triplea.java.collections.IntegerMap;

/** A collection of methods for tracking which players have which technology advances. */
@AllArgsConstructor
public class TechTracker {
  private final GameData data;

  @Value
  static class Key {
    GamePlayer player;
    UnitType unitType;
    String property;
  }

  private final Map<Key, Object> cache = new ConcurrentHashMap<>();

  public void clearCache() {
    cache.clear();
  }

  public int getAirDefenseBonus(GamePlayer player, UnitType type) {
    final Supplier<Integer> getter =
        () -> getSumOfBonuses(TechAbilityAttachment::getAirDefenseBonus, type, player);
    return getCached(player, type, "getAirDefenseBonus", getter);
  }

  public int getAirAttackBonus(GamePlayer player, UnitType type) {
    final Supplier<Integer> getter =
        () -> getSumOfBonuses(TechAbilityAttachment::getAirAttackBonus, type, player);
    return getCached(player, type, "getAirAttackBonus", getter);
  }

  public int getMovementBonus(GamePlayer player, UnitType type) {
    final Supplier<Integer> getter =
        () -> getSumOfBonuses(TechAbilityAttachment::getMovementBonus, type, player);
    return getCached(player, type, "getMovementBonus", getter);
  }

  public int getAttackBonus(GamePlayer player, UnitType type) {
    final Supplier<Integer> getter =
        () -> getSumOfBonuses(TechAbilityAttachment::getAttackBonus, type, player);
    return getCached(player, type, "getAttackBonus", getter);
  }

  public int getAttackRollsBonus(GamePlayer player, UnitType type) {
    final Supplier<Integer> getter =
        () -> getSumOfBonuses(TechAbilityAttachment::getAttackRollsBonus, type, player);
    return getCached(player, type, "getAttackRollsBonus", getter);
  }

  public int getDefenseBonus(GamePlayer player, UnitType type) {
    final Supplier<Integer> getter =
        () -> getSumOfBonuses(TechAbilityAttachment::getDefenseBonus, type, player);
    return getCached(player, type, "getDefenseBonus", getter);
  }

  public int getDefenseRollsBonus(GamePlayer player, UnitType type) {
    final Supplier<Integer> getter =
        () -> getSumOfBonuses(TechAbilityAttachment::getDefenseRollsBonus, type, player);
    return getCached(player, type, "getDefenseRollsBonus", getter);
  }

  public int getRadarBonus(GamePlayer player, UnitType type) {
    final Supplier<Integer> getter =
        () -> getSumOfBonuses(TechAbilityAttachment::getRadarBonus, type, player);
    return getCached(player, type, "getRadarBonus", getter);
  }

  public int getRocketDiceNumber(GamePlayer player, UnitType type) {
    final Supplier<Integer> getter =
        () -> getSumOfBonuses(TechAbilityAttachment::getRocketDiceNumber, type, player);
    return getCached(player, type, "getRocketDiceNumber", getter);
  }

  public int getBombingBonus(GamePlayer player, UnitType type) {
    final Supplier<Integer> getter =
        () -> getSumOfBonuses(TechAbilityAttachment::getBombingBonus, type, player);
    return getCached(player, type, "getBombingBonus", getter);
  }

  public int getProductionBonus(GamePlayer player, UnitType type) {
    final Supplier<Integer> getter =
        () -> getSumOfBonuses(TechAbilityAttachment::getProductionBonus, type, player);
    return getCached(player, type, "getProductionBonus", getter);
  }

  public boolean canBlitz(GamePlayer player, UnitType type) {
    final BooleanSupplier getter =
        () -> getUnitAbilitiesGained(TechAbilityAttachment.ABILITY_CAN_BLITZ, type, player);
    return getCached(player, type, "canBlitz", getter);
  }

  public boolean canBombard(GamePlayer player, UnitType type) {
    final BooleanSupplier getter =
        () -> getUnitAbilitiesGained(TechAbilityAttachment.ABILITY_CAN_BOMBARD, type, player);
    return getCached(player, type, "canBombard", getter);
  }

  public int tuv(GamePlayer player, UnitType type) {
    //sum will be 0 if there are no TUV bonus
    //the return value must be checked against -1 calculate
    final Supplier<Integer> getter =
        () -> getSumOfBonuses(TechAbilityAttachment::getTUVBonus, type, player);
    return getCached(player, type, "getTUVBonus", getter);
  }

  public int getMinimumTerritoryValueForProductionBonus(final GamePlayer player) {
    final Supplier<Integer> getter =
        () ->
            Math.max(
                0,
                getCurrentTechAdvances(player).stream()
                    .map(TechAbilityAttachment::get)
                    .filter(Objects::nonNull)
                    .mapToInt(TechAbilityAttachment::getMinimumTerritoryValueForProductionBonus)
                    .filter(i -> i != -1)
                    .min()
                    .orElse(-1));
    return getCached(player, null, "getMinimumTerritoryValueForProductionBonus", getter);
  }

  public int getRocketNumberPerTerritory(final GamePlayer player) {
    final Supplier<Integer> getter =
        () ->
            sumNumbers(
                TechAbilityAttachment::getRocketNumberPerTerritory,
                TechAdvance.TECH_NAME_ROCKETS,
                getCurrentTechAdvances(player));
    return getCached(player, null, "getRocketNumberPerTerritory", getter);
  }

  public int getRocketDistance(final GamePlayer player) {
    final Supplier<Integer> getter =
        () ->
            sumNumbers(
                TechAbilityAttachment::getRocketDistance,
                TechAdvance.TECH_NAME_ROCKETS,
                getCurrentTechAdvances(player));
    return getCached(player, null, "getRocketDistance", getter);
  }

  private int getCached(
      GamePlayer player, UnitType type, String property, Supplier<Integer> getter) {
    return (Integer) cache.computeIfAbsent(new Key(player, type, property), key -> getter.get());
  }

  private boolean getCached(
      GamePlayer player, UnitType type, String property, BooleanSupplier getter) {
    return (Boolean)
        cache.computeIfAbsent(new Key(player, type, property), key -> getter.getAsBoolean());
  }

  private int getSumOfBonuses(
      Function<TechAbilityAttachment, IntegerMap<UnitType>> mapper,
      UnitType type,
      GamePlayer player) {
    return sumIntegerMap(mapper, type, getCurrentTechAdvances(player));
  }

  static int sumIntegerMap(
      final Function<TechAbilityAttachment, IntegerMap<UnitType>> mapper,
      final UnitType ut,
      final Collection<TechAdvance> techAdvances) {
    return techAdvances.stream()
        .map(TechAbilityAttachment::get)
        .filter(Objects::nonNull)
        .map(mapper)
        .mapToInt(m -> m.getInt(ut))
        .sum();
  }

  private boolean getUnitAbilitiesGained(
      String filterForAbility, UnitType unitType, GamePlayer player) {
    return getCurrentTechAdvances(player).stream()
        .map(TechAbilityAttachment::get)
        .filter(Objects::nonNull)
        .map(TechAbilityAttachment::getUnitAbilitiesGained)
        .map(m -> m.get(unitType))
        .filter(Objects::nonNull)
        .flatMap(Collection::stream)
        .anyMatch(filterForAbility::equals);
  }

  @VisibleForTesting
  static int sumNumbers(
      final ToIntFunction<TechAbilityAttachment> mapper,
      final String attachmentType,
      final Collection<TechAdvance> techAdvances) {
    return techAdvances.stream()
        .map(TechAbilityAttachment::get)
        .filter(Objects::nonNull)
        .filter(i -> i.getAttachedTo().toString().equals(attachmentType))
        .mapToInt(mapper)
        .filter(i -> i > 0)
        .sum();
  }

  private Collection<TechAdvance> getCurrentTechAdvances(GamePlayer player) {
    return getCurrentTechAdvances(player, data.getTechnologyFrontier());
  }

  /**
   * Returns what tech advances this player already has successfully researched (including ones that
   * may not be in their tech frontier).
   */
  public static Collection<TechAdvance> getCurrentTechAdvances(
      final GamePlayer gamePlayer, final TechnologyFrontier technologyFrontier) {
    final TechAttachment attachment = gamePlayer.getTechAttachment();
    // search all techs
    return TechAdvance.getTechAdvances(technologyFrontier).stream()
        .filter(ta -> ta.hasTech(attachment))
        .collect(Collectors.toList());
  }

  /**
   * Returns what tech categories are no longer available for this player, because all techs in them
   * have been successfully researched already.
   */
  public static Collection<TechnologyFrontier> getFullyResearchedPlayerTechCategories(
      final GamePlayer gamePlayer) {
    final Collection<TechnologyFrontier> technologyFrontiers = new ArrayList<>();
    final TechAttachment attachment = gamePlayer.getTechAttachment();
    for (final TechnologyFrontier tf : TechAdvance.getPlayerTechCategories(gamePlayer)) {
      if (tf.getTechs().stream().allMatch(t -> t.hasTech(attachment))) {
        technologyFrontiers.add(tf);
      }
    }
    return technologyFrontiers;
  }

  /** Grants or adds a tech advance to a given player. */
  public static void addAdvance(
      final GamePlayer player, final IDelegateBridge bridge, final TechAdvance advance) {
    bridge.addChange(createTechChange(advance, player, true));
    advance.perform(player, bridge);
  }

  static void removeAdvance(
      final GamePlayer player, final IDelegateBridge bridge, final TechAdvance advance) {
    bridge.addChange(createTechChange(advance, player, false));
  }

  private static Change createTechChange(
      final TechAdvance advance, final GamePlayer player, final boolean value) {
    final TechAttachment attachment = player.getTechAttachment();
    if (advance instanceof GenericTechAdvance
        && ((GenericTechAdvance) advance).getAdvance() == null) {
      return ChangeFactory.genericTechChange(attachment, value, advance.getProperty());
    }
    return ChangeFactory.attachmentPropertyChange(
        attachment, String.valueOf(value), advance.getProperty());
  }

  public static int getTechCost(final GamePlayer player) {
    return player.getTechAttachment().getTechCost();
  }

  public static boolean hasLongRangeAir(final GamePlayer player) {
    return player.getTechAttachment().getLongRangeAir();
  }

  public static boolean hasHeavyBomber(final GamePlayer player) {
    return player.getTechAttachment().getHeavyBomber();
  }

  public static boolean hasSuperSubs(final GamePlayer player) {
    return player.getTechAttachment().getSuperSub();
  }

  public static boolean hasJetFighter(final GamePlayer player) {
    return player.getTechAttachment().getJetPower();
  }

  public static boolean hasRocket(final GamePlayer player) {
    return player.getTechAttachment().getRocket();
  }

  public static boolean hasIndustrialTechnology(final GamePlayer player) {
    return player.getTechAttachment().getIndustrialTechnology();
  }

  public static boolean hasImprovedArtillerySupport(final GamePlayer player) {
    return player.getTechAttachment().getImprovedArtillerySupport();
  }

  public static boolean hasParatroopers(final GamePlayer player) {
    return player.getTechAttachment().getParatroopers();
  }

  public static boolean hasIncreasedFactoryProduction(final GamePlayer player) {
    return player.getTechAttachment().getIncreasedFactoryProduction();
  }

  public static boolean hasAaRadar(final GamePlayer player) {
    return player.getTechAttachment().getAaRadar();
  }
}
