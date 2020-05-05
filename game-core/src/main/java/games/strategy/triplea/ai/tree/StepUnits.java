package games.strategy.triplea.ai.tree;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitAttachment;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.Getter;

public class StepUnits implements Cloneable, Comparable<StepUnits> {

  @Getter private final BattleStep.Type type;
  @Getter private final GamePlayer player;
  private final BitSet friendlyBits;
  private final List<Unit> friendlyUnits;
  private final Map<Unit, Double> friendlyUnitsChances = new LinkedHashMap<>();
  private final BitSet friendlyWaitingToDieBits;
  private final Map<Unit, Integer> friendlyHitPoints;
  private final List<Unit> retreatedFriendly;
  @Getter private final GamePlayer enemy;
  private final BitSet enemyBits;
  private final List<Unit> enemyUnits;
  private final Map<Unit, Double> enemyUnitsChances = new LinkedHashMap<>();
  private final BitSet enemyWaitingToDieBits;
  private final Map<Unit, Integer> enemyHitPoints;
  private final List<Unit> retreatedEnemy;
  @Getter private double probability;

  StepUnits(final List<Unit> friendlyUnits, final GamePlayer player, final List<Unit> enemyUnits, final GamePlayer enemy) {
    this.type = BattleStep.Type.AA_ATTACKER;
    this.friendlyBits = new BitSet(friendlyUnits.size());
    this.friendlyWaitingToDieBits = new BitSet(friendlyUnits.size());
    this.player = player;
    this.friendlyUnits = friendlyUnits;
    this.retreatedFriendly = new ArrayList<>();
    this.friendlyHitPoints = new HashMap<>();
    for (final Unit unit : this.friendlyUnits) {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (ua.getHitPoints() > 1) {
        this.friendlyHitPoints.put(unit, ua.getHitPoints());
      }
    }

    this.enemyBits = new BitSet(enemyUnits.size());
    this.enemyWaitingToDieBits = new BitSet(enemyUnits.size());
    this.enemy = enemy;
    this.enemyUnits = enemyUnits;
    this.retreatedEnemy = new ArrayList<>();
    this.enemyHitPoints = new HashMap<>();
    for (final Unit unit : this.enemyUnits) {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      if (ua.getHitPoints() > 1) {
        this.enemyHitPoints.put(unit, ua.getHitPoints());
      }
    }
    this.probability = 0.0;
  }

  StepUnits(final StepUnits other) {
    this(other, 0.0);
  }

  StepUnits(final StepUnits other, final double probability) {
    this(
        other.type,
        other.friendlyBits,
        other.player,
        other.friendlyUnits,
        other.friendlyWaitingToDieBits,
        other.friendlyHitPoints,
        other.retreatedFriendly,
        other.enemyBits,
        other.enemy,
        other.enemyUnits,
        other.enemyWaitingToDieBits,
        other.enemyHitPoints,
        other.retreatedEnemy,
        probability);
  }

  private StepUnits(
      final BattleStep.Type type,
      final BitSet friendlyBits,
      final GamePlayer player,
      final List<Unit> friendlyUnits,
      final BitSet friendlyWaitingToDieBits,
      final Map<Unit, Integer> friendlyHitPoints,
      final List<Unit> retreatedFriendly,
      final BitSet enemyBits,
      final GamePlayer enemy,
      final List<Unit> enemyUnits,
      final BitSet enemyWaitingToDieBits,
      final Map<Unit, Integer> enemyHitPoints,
      final List<Unit> retreatedEnemy,
      final double probability
  ) {
    this.type = type;

    this.player = player;
    this.friendlyBits = (BitSet) friendlyBits.clone();
    this.friendlyWaitingToDieBits = (BitSet) friendlyWaitingToDieBits.clone();
    this.friendlyUnits = friendlyUnits;
    this.friendlyHitPoints = new HashMap<>(friendlyHitPoints);
    this.retreatedFriendly = new ArrayList<>(retreatedFriendly);

    this.enemy = enemy;
    this.enemyBits = (BitSet) enemyBits.clone();
    this.enemyWaitingToDieBits = (BitSet) enemyWaitingToDieBits.clone();
    this.enemyUnits = enemyUnits;
    this.enemyHitPoints = new HashMap<>(enemyHitPoints);
    this.retreatedEnemy = new ArrayList<>(retreatedEnemy);

    this.probability = probability;
  }

  StepUnits swapSides() {
    final BattleStep.Type newType = type.nextType();

    // swapping the enemies and friendlies as we are swapping sides
    return new StepUnits(
        newType,
        enemyBits,
        enemy,
        enemyUnits,
        enemyWaitingToDieBits,
        enemyHitPoints,
        retreatedEnemy,
        friendlyBits,
        player,
        friendlyUnits,
        friendlyWaitingToDieBits,
        friendlyHitPoints,
        retreatedFriendly,
        0.0
    );
  }

  StepUnits removeWaitingToDie() {
    final StepUnits units = new StepUnits(this);
    units.friendlyBits.or(friendlyWaitingToDieBits);
    units.enemyBits.or(enemyWaitingToDieBits);
    units.friendlyWaitingToDieBits.or(friendlyBits);
    units.enemyWaitingToDieBits.or(enemyBits);
    return units;
  }

  void addProbability(final double other) {
    probability += other;
  }

  void hitFriendly(final Unit friendly) {
    hit(friendly, friendlyUnits.indexOf(friendly), friendlyWaitingToDieBits, friendlyHitPoints);
  }

  void hitEnemy(final Unit enemy) {
    hit(enemy, enemyUnits.indexOf(enemy), enemyWaitingToDieBits, enemyHitPoints);
  }

  private void hit(final Unit unit, final int unitIndex, final BitSet waitingToDie, final Map<Unit, Integer> hitPointsMap) {
    hitPointsMap.compute(unit, (key, hitPoints) -> {
      if (hitPoints == null) {
        waitingToDie.set(unitIndex);
        return null;
      } else {
        hitPoints--;
        if (hitPoints == 0) {
          waitingToDie.set(unitIndex);
          return null;
        } else {
          hitPointsMap.put(unit, hitPoints);
          return hitPoints;
        }
      }
    });
  }

  void retreatFriendly(Unit friendly) {
    hit(friendly, friendlyUnits.indexOf(friendly), friendlyWaitingToDieBits, new HashMap<>());
    hit(friendly, friendlyUnits.indexOf(friendly), friendlyBits, new HashMap<>());
    retreatedFriendly.add(friendly);
  }

  void retreatEnemy(Unit enemy) {
    hit(enemy, enemyUnits.indexOf(enemy), enemyWaitingToDieBits, new HashMap<>());
    hit(enemy, enemyUnits.indexOf(enemy), enemyBits, new HashMap<>());
    retreatedEnemy.add(enemy);
  }

  List<Unit> getAliveOrWaitingToDieFriendly() {
    return filterUnits(this.friendlyUnits, this.friendlyBits, this.friendlyUnits.size());
  }

  List<Unit> getAliveFriendly() {
    return filterUnits(this.friendlyUnits, this.friendlyWaitingToDieBits, this.friendlyUnits.size());
  }

  List<Unit> getAliveOrWaitingToDieEnemy() {
    return filterUnits(this.enemyUnits, this.enemyBits, this.enemyUnits.size());
  }

  List<Unit> getAliveEnemy() {
    return filterUnits(this.enemyUnits, this.enemyWaitingToDieBits, this.enemyUnits.size());
  }

  private List<Unit> filterUnits(final List<Unit> units, final BitSet bitSet, final int count) {
    final List<Unit> filteredUnits = new ArrayList<>();
    int pos = bitSet.nextClearBit(0);
    while (pos < count) {
      filteredUnits.add(units.get(pos));
      pos = bitSet.nextClearBit(pos + 1);
    }
    return filteredUnits;
  }

  List<Unit> addFriendlyMultiHitTargets(final List<Unit> targets) {
    final List<Unit> newTargets;
    if (friendlyHitPoints.isEmpty()) {
      newTargets = targets;
    } else {
      newTargets = new ArrayList<>(targets);
      for (final Map.Entry<Unit, Integer> entry : friendlyHitPoints.entrySet()) {
        if (!targets.contains(entry.getKey())) {
          continue;
        }
        for (int i = 0; i < entry.getValue() - 1; i++) {
          newTargets.add(0, entry.getKey());
        }
      }
    }
    return newTargets;
  }

  boolean noMoreFriendlies() {
    return countOfFriendlyDamagedOrDead() == friendlyUnits.size();
  }

  boolean noMoreEnemies() {
    return countOfEnemyDamagedOrDead() == enemyUnits.size();
  }

  int countOfFriendliesNotDamagedOrDead() {
    return friendlyUnits.size() - countOfFriendlyDamagedOrDead();
  }

  int countOfFriendliesNotDead() {
    return friendlyUnits.size() - countOfFriendlyDead();
  }

  int countOfEnemiesNotDamagedOrDead() {
    return enemyUnits.size() - countOfEnemyDamagedOrDead();
  }

  int countOfEnemiesNotDead() {
    return enemyUnits.size() - countOfEnemyDead();
  }

  int countOfFriendlyDamagedOrDead() {
    return friendlyWaitingToDieBits.cardinality();
  }

  int countOfFriendlyDead() {
    return friendlyBits.cardinality();
  }

  int countOfEnemyDamagedOrDead() {
    return enemyWaitingToDieBits.cardinality();
  }

  int countOfEnemyDead() {
    return enemyBits.cardinality();
  }

  int countOfFriendlyHitPoints() {
    return friendlyHitPoints.values().stream().reduce(0, Integer::sum);
  }

  /**
   * Store whether the units were alive
   */
  void recordChances() {
    for (final Unit unit : getAliveFriendly()) {
      friendlyUnitsChances.put(unit, 1.0);
    }
    for (final Unit unit : retreatedFriendly) {
      friendlyUnitsChances.put(unit, 1.0);
    }
    for (final Unit unit : getAliveEnemy()) {
      enemyUnitsChances.put(unit, 1.0);
    }
    for (final Unit unit : retreatedEnemy) {
      enemyUnitsChances.put(unit, 1.0);
    }
  }

  void updateUnitChances(final StepUnits child, final double probability) {
    for (final Map.Entry<Unit, Double> entry : child.enemyUnitsChances.entrySet()) {
      friendlyUnitsChances.compute(entry.getKey(), (key, value) -> {
        if (value == null) {
          return entry.getValue() * probability;
        } else {
          return value + entry.getValue() * probability;
        }
      });
    }
    for (final Map.Entry<Unit, Double> entry : child.friendlyUnitsChances.entrySet()) {
      enemyUnitsChances.compute(entry.getKey(), (key, value) -> {
        if (value == null) {
          return entry.getValue() * probability;
        } else {
          return value + entry.getValue() * probability;
        }
      });
    }
  }

  List<Unit> getFriendlyWithChance(final double chance) {
    return friendlyUnits.stream()
        .filter(unit -> friendlyUnitsChances.getOrDefault(unit, 0.0) > chance)
        .collect(Collectors.toList());
  }

  List<Unit> getEnemyWithChance(final double chance) {
    return enemyUnits.stream()
        .filter(unit -> enemyUnitsChances.getOrDefault(unit, 0.0) > chance)
        .collect(Collectors.toList());
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof StepUnits)) return false;
    final StepUnits units = (StepUnits) o;
    return type == units.type
        && Objects.equals(friendlyBits, units.friendlyBits)
        && Objects.equals(enemyBits, units.enemyBits)
        && Objects.equals(friendlyWaitingToDieBits, units.friendlyWaitingToDieBits)
        && Objects.equals(enemyWaitingToDieBits, units.enemyWaitingToDieBits)
        && Objects.equals(friendlyHitPoints, units.friendlyHitPoints)
        && Objects.equals(enemyHitPoints, units.enemyHitPoints);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, friendlyBits, enemyBits, friendlyWaitingToDieBits, enemyWaitingToDieBits);
  }

  @Override
  public int compareTo(final StepUnits o) {
    if (!type.equals(o.type)) {
      return type.compareTo(o.type);
    }
    if (!friendlyBits.equals(o.friendlyBits)) {
      return friendlyBits.toString().compareTo(o.friendlyBits.toString());
    }
    if (!enemyBits.equals(o.enemyBits)) {
      return enemyBits.toString().compareTo(o.enemyBits.toString());
    }
    if (!friendlyWaitingToDieBits.equals(o.friendlyWaitingToDieBits)) {
      return friendlyWaitingToDieBits.toString().compareTo(o.friendlyWaitingToDieBits.toString());
    }
    if (!enemyWaitingToDieBits.equals(o.enemyWaitingToDieBits)) {
      return enemyWaitingToDieBits.toString().compareTo(o.enemyWaitingToDieBits.toString());
    }
    return 0;
  }

  @Override
  public String toString() {
    return "Units{" +
        "type=" + type +
        ", friendlyDead=" + friendlyBits +
        ", friendlyInjured=" + friendlyWaitingToDieBits +
        ", friendliesCount=" + friendlyUnits.size() +
        ", friendlyHitPoints=" + friendlyHitPoints +
        ", enemyDead=" + enemyBits +
        ", enemyInjured=" + enemyWaitingToDieBits +
        ", enemiesCount=" + enemyUnits.size() +
        ", enemyHitPoints =" + enemyHitPoints +
        ", probability=" + probability +
        '}';
  }
}
