package games.strategy.triplea.attachments;

import java.util.Comparator;

import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.Matches;

public class UnitTypeComparator implements Comparator<UnitType> {
  @Override
  public int compare(final UnitType o1, final UnitType o2) {
    final UnitType u1 = o1;
    final UnitType u2 = o2;
    final UnitAttachment ua1 = UnitAttachment.get(u1);
    final UnitAttachment ua2 = UnitAttachment.get(u2);
    if (ua1 == null) {
      throw new IllegalStateException("No unit type attachment for unit type : " + u1.getName());
    }
    if (ua2 == null) {
      throw new IllegalStateException("No unit type attachment for unit type : " + u2.getName());
    }
    if (ua1.getIsInfrastructure() && !ua2.getIsInfrastructure()) {
      return 1;
    }
    if (ua2.getIsInfrastructure() && !ua1.getIsInfrastructure()) {
      return -1;
    }
    if (Matches.UnitTypeIsAAforAnything.match(u1) && !Matches.UnitTypeIsAAforAnything.match(u2)) {
      return 1;
    }
    if (!Matches.UnitTypeIsAAforAnything.match(u1) && Matches.UnitTypeIsAAforAnything.match(u2)) {
      return -1;
    }
    if (ua1.getIsAir() && !ua2.getIsAir()) {
      return 1;
    }
    if (ua2.getIsAir() && !ua1.getIsAir()) {
      return -1;
    }
    if (ua1.getIsSea() && !ua2.getIsSea()) {
      return 1;
    }
    if (ua2.getIsSea() && !ua1.getIsSea()) {
      return -1;
    }
    if (ua1.getRawAttack() != ua2.getRawAttack()) {
      return ua1.getRawAttack() - ua2.getRawAttack();
    }
    return u1.getName().compareTo(u2.getName());
  }
}
