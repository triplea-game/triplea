package games.strategy.engine.data;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** A prototype for units. */
public class UnitType extends NamedAttachable {
  private static final long serialVersionUID = 4885339076798905247L;

  public UnitType(final String name, final GameData data) {
    super(name, data);
  }

  public List<Unit> create(final int quantity, final GamePlayer owner) {
    return create(quantity, owner, false, 0, 0);
  }

  public List<Unit> createTemp(final int quantity, final GamePlayer owner) {
    return create(quantity, owner, true, 0, 0);
  }

  public List<Unit> create(
      final int quantity,
      final GamePlayer owner,
      final boolean isTemp,
      final int hitsTaken,
      final int bombingUnitDamage) {
    return IntStream.range(0, quantity)
        .mapToObj(i -> create(owner, isTemp, hitsTaken, bombingUnitDamage))
        .collect(Collectors.toList());
  }

  private Unit create(
      final GamePlayer owner,
      final boolean isTemp,
      final int hitsTaken,
      final int bombingUnitDamage) {
    final Unit u = new Unit(this, owner, getData());
    u.setHits(hitsTaken);
    u.setUnitDamage(bombingUnitDamage);
    if (!isTemp) {
      getData().getUnits().put(u);
    }
    return u;
  }

  public Unit create(final GamePlayer owner) {
    return create(owner, false, 0, 0);
  }

  @Override
  public boolean equals(final Object o) {
    return o instanceof UnitType && ((UnitType) o).getName().equals(getName());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getName());
  }
}
