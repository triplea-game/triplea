package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import java.util.Map;
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
  UsedSupportTracker tracker = new UsedSupportTracker();

  AaRoll(final AvailableSupports supportFromFriends, final AvailableSupports supportFromEnemies) {
    this.supportFromFriends = supportFromFriends.filter(UnitSupportAttachment::getAaRoll);
    this.supportFromEnemies = supportFromEnemies.filter(UnitSupportAttachment::getAaRoll);
  }

  @Override
  public RollValue getRoll(final Unit unit) {
    return RollValue.of(unit.getUnitAttachment().getMaxAaAttacks())
        .add(tracker.giveSupport(unit, supportFromFriends))
        .add(tracker.giveSupport(unit, supportFromEnemies));
  }

  @Override
  public Map<Unit, IntegerMap<Unit>> getSupportGiven() {
    return tracker.getSupportGiven();
  }
}
