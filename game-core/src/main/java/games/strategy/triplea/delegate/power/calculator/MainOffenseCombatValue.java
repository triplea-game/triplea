package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import org.triplea.java.collections.IntegerMap;

/**
 * Calculates offense strength and roll for non-AA dice
 *
 * <p>This takes into account marine, bombarding, territory effects, friendly support, and enemy
 * support
 */
@Builder
@Value
@Getter(AccessLevel.NONE)
class MainOffenseCombatValue implements CombatValue {

  @Getter(onMethod = @__({@Override}))
  @NonNull
  GameData gameData;

  @NonNull AvailableSupports supportFromFriends;
  @NonNull AvailableSupports supportFromEnemies;

  @NonNull Collection<TerritoryEffect> territoryEffects;

  @Getter(onMethod = @__({@Override}))
  @NonNull
  @Builder.Default
  Collection<Unit> friendUnits = List.of();

  @Getter(onMethod = @__({@Override}))
  @NonNull
  @Builder.Default
  Collection<Unit> enemyUnits = List.of();

  @Override
  public RollCalculator getRoll() {
    return new MainOffenseRoll(supportFromFriends, supportFromEnemies);
  }

  @Override
  public StrengthCalculator getStrength() {
    return new MainOffenseStrength(
        gameData, supportFromFriends, supportFromEnemies, territoryEffects);
  }

  @Override
  public int getDiceSides(final Unit unit) {
    return gameData.getDiceSides();
  }

  @Override
  public BattleState.Side getBattleSide() {
    return BattleState.Side.OFFENSE;
  }

  @Override
  public boolean chooseBestRoll(final Unit unit) {
    return Properties.getLhtrHeavyBombers(gameData.getProperties())
        || unit.getUnitAttachment().getChooseBestRoll();
  }

  @Override
  public CombatValue buildWithNoUnitSupports() {
    return MainOffenseCombatValue.builder()
        .gameData(gameData)
        .supportFromFriends(AvailableSupports.EMPTY_RESULT)
        .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
        .friendUnits(List.of())
        .enemyUnits(List.of())
        .territoryEffects(territoryEffects)
        .build();
  }

  @Override
  public CombatValue buildOppositeCombatValue() {
    return MainDefenseCombatValue.builder()
        .gameData(gameData)
        .supportFromFriends(supportFromEnemies)
        .supportFromEnemies(supportFromFriends)
        .friendUnits(enemyUnits)
        .enemyUnits(friendUnits)
        .territoryEffects(territoryEffects)
        .build();
  }

  @Value
  static class MainOffenseRoll implements RollCalculator {

    AvailableSupports supportFromFriends;
    AvailableSupports supportFromEnemies;

    MainOffenseRoll(
        final AvailableSupports supportFromFriends, final AvailableSupports supportFromEnemies) {
      this.supportFromFriends = supportFromFriends.filter(UnitSupportAttachment::getRoll);
      this.supportFromEnemies = supportFromEnemies.filter(UnitSupportAttachment::getRoll);
    }

    @Override
    public RollValue getRoll(final Unit unit) {
      return RollValue.of(unit.getUnitAttachment().getAttackRolls(unit.getOwner()))
          .add(supportFromFriends.giveSupportToUnit(unit))
          .add(supportFromEnemies.giveSupportToUnit(unit));
    }

    @Override
    public Map<Unit, IntegerMap<Unit>> getSupportGiven() {
      return Stream.of(
              supportFromFriends.getUnitsGivingSupport(),
              supportFromEnemies.getUnitsGivingSupport())
          .flatMap(map -> map.entrySet().stream())
          .collect(
              Collectors.toMap(
                  Map.Entry::getKey,
                  Map.Entry::getValue,
                  (value1, value2) -> {
                    final IntegerMap<Unit> merged = new IntegerMap<>(value1);
                    merged.add(value2);
                    return merged;
                  }));
    }
  }

  @Value
  static class MainOffenseStrength implements StrengthCalculator {

    GameData gameData;
    Collection<TerritoryEffect> territoryEffects;
    AvailableSupports supportFromFriends;
    AvailableSupports supportFromEnemies;

    MainOffenseStrength(
        final GameData gameData,
        final AvailableSupports supportFromFriends,
        final AvailableSupports supportFromEnemies,
        final Collection<TerritoryEffect> territoryEffects) {
      this.gameData = gameData;
      this.territoryEffects = territoryEffects;
      this.supportFromFriends = supportFromFriends.filter(UnitSupportAttachment::getStrength);
      this.supportFromEnemies = supportFromEnemies.filter(UnitSupportAttachment::getStrength);
    }

    @Override
    public StrengthValue getStrength(final Unit unit) {
      final UnitAttachment ua = unit.getUnitAttachment();
      int strength = ua.getAttack(unit.getOwner());
      if (ua.getIsMarine() != 0 && unit.getWasAmphibious()) {
        strength += ua.getIsMarine();
      }

      return StrengthValue.of(gameData.getDiceSides(), strength)
          .add(
              TerritoryEffectHelper.getTerritoryCombatBonus(
                  unit.getType(), territoryEffects, false))
          .add(supportFromFriends.giveSupportToUnit(unit))
          .add(supportFromEnemies.giveSupportToUnit(unit));
    }

    @Override
    public Map<Unit, IntegerMap<Unit>> getSupportGiven() {
      return Stream.of(
              supportFromFriends.getUnitsGivingSupport(),
              supportFromEnemies.getUnitsGivingSupport())
          .flatMap(map -> map.entrySet().stream())
          .collect(
              Collectors.toMap(
                  Map.Entry::getKey,
                  Map.Entry::getValue,
                  (value1, value2) -> {
                    final IntegerMap<Unit> merged = new IntegerMap<>(value1);
                    merged.add(value2);
                    return merged;
                  }));
    }
  }
}
