package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
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
public class BombardmentCombatValue implements CombatValue {

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
    return new MainOffenseCombatValue.MainOffenseRoll(supportFromFriends, supportFromEnemies);
  }

  @Override
  public StrengthCalculator getStrength() {
    return new BombardmentStrength(
        gameData, supportFromFriends, supportFromEnemies, territoryEffects);
  }

  @Override
  public int getDiceSides(final Unit unit) {
    return gameData.getDiceSides();
  }

  @Override
  public boolean isDefending() {
    return false;
  }

  @Override
  public boolean chooseBestRoll(final Unit unit) {
    return Properties.getLhtrHeavyBombers(gameData.getProperties())
        || unit.getUnitAttachment().getChooseBestRoll();
  }

  @Override
  public CombatValue buildWithNoUnitSupports() {
    return BombardmentCombatValue.builder()
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
    return BombardmentCombatValue.builder()
        .gameData(gameData)
        .supportFromFriends(supportFromEnemies)
        .supportFromEnemies(supportFromFriends)
        .friendUnits(enemyUnits)
        .enemyUnits(friendUnits)
        .territoryEffects(territoryEffects)
        .build();
  }

  @Value
  static class BombardmentStrength implements StrengthCalculator {

    GameData gameData;
    Collection<TerritoryEffect> territoryEffects;
    AvailableSupports supportFromFriends;
    AvailableSupports supportFromEnemies;

    BombardmentStrength(
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
      final int strength = ua.getBombard();

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
