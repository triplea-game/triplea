package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitSupportAttachment;
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
 * Calculates defense strength and roll for AA/Targeted dice
 *
 * <p>This takes into account friendly support, and enemy support
 */
@Builder
@Value
@Getter(AccessLevel.NONE)
class AaDefenseCombatValue implements CombatValue {

  @Getter(onMethod = @__({@Override}))
  @NonNull
  GameData gameData;

  @NonNull AvailableSupports supportFromFriends;
  @NonNull AvailableSupports supportFromEnemies;

  @Getter(onMethod = @__({@Override}))
  @NonNull
  @Builder.Default
  Collection<Unit> friendUnits = List.of();

  @Getter(onMethod = @__({@Override}))
  @NonNull
  @Builder.Default
  Collection<Unit> enemyUnits = List.of();

  @Getter(onMethod = @__({@Override}))
  Collection<TerritoryEffect> territoryEffects = List.of();

  @Override
  public RollCalculator getRoll() {
    return new AaRoll(supportFromFriends, supportFromEnemies);
  }

  @Override
  public StrengthCalculator getStrength() {
    return new AaDefenseStrength(gameData, supportFromFriends, supportFromEnemies);
  }

  @Override
  public boolean isDefending() {
    return true;
  }

  @Value
  static class AaDefenseStrength implements StrengthCalculator {

    GameData gameData;
    AvailableSupports supportFromFriends;
    AvailableSupports supportFromEnemies;

    AaDefenseStrength(
        final GameData gameData,
        final AvailableSupports supportFromFriends,
        final AvailableSupports supportFromEnemies) {
      this.gameData = gameData;
      this.supportFromFriends = supportFromFriends.filter(UnitSupportAttachment::getAaStrength);
      this.supportFromEnemies = supportFromEnemies.filter(UnitSupportAttachment::getAaStrength);
    }

    @Override
    public StrengthValue getStrength(final Unit unit) {
      return StrengthValue.of(
              gameData.getDiceSides(), unit.getUnitAttachment().getAttackAa(unit.getOwner()))
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
