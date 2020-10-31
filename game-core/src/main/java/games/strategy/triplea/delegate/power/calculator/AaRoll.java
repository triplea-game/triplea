package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import org.triplea.java.collections.IntegerMap;

/** Calculate the roll for AA dice */
@Value
@Getter(AccessLevel.NONE)
class AaRoll implements RollCalculator {

  AvailableSupports supportFromFriends;
  AvailableSupports supportFromEnemies;

  AaRoll(final AvailableSupports supportFromFriends, final AvailableSupports supportFromEnemies) {
    this.supportFromFriends = supportFromFriends.filter(UnitSupportAttachment::getAaRoll);
    this.supportFromEnemies = supportFromEnemies.filter(UnitSupportAttachment::getAaRoll);
  }

  @Override
  public RollValue getRoll(final Unit unit) {
    return RollValue.of(unit.getUnitAttachment().getMaxAaAttacks())
        .add(supportFromFriends.giveSupportToUnit(unit))
        .add(supportFromEnemies.giveSupportToUnit(unit));
  }

  @Override
  public Map<Unit, IntegerMap<Unit>> getSupportGiven() {
    return Stream.of(
            supportFromFriends.getUnitsGivingSupport(), supportFromEnemies.getUnitsGivingSupport())
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
