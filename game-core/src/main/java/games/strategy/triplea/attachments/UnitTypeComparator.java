package games.strategy.triplea.attachments;

import java.io.Serializable;
import java.util.Comparator;

import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.Matches;

/**
 * A comparator that sorts unit types in a meaningful order.
 *
 * <p>The order is based on the following attributes of the unit type:
 *
 * <ul>
 *   <li>infrastructure
 *   <li>anti-aircraft
 *   <li>air
 *   <li>sea
 *   <li>attack power
 *   <li>name
 * </ul>
 */
public class UnitTypeComparator implements Comparator<UnitType>, Serializable {
  private static final long serialVersionUID = -7065456161376169082L;

  @Override
  public int compare(final UnitType u1, final UnitType u2) {
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
    if (Matches.unitTypeIsAaForAnything().test(u1) && !Matches.unitTypeIsAaForAnything().test(u2)) {
      return 1;
    }
    if (!Matches.unitTypeIsAaForAnything().test(u1) && Matches.unitTypeIsAaForAnything().test(u2)) {
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
    if (ua1.getAttack() != ua2.getAttack()) {
      return ua1.getAttack() - ua2.getAttack();
    }
    return u1.getName().compareTo(u2.getName());
  }
}
