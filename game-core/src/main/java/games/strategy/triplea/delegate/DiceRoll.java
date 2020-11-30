package games.strategy.triplea.delegate;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Die.DieType;
import games.strategy.triplea.delegate.power.calculator.AaPowerStrengthAndRolls;
import games.strategy.triplea.delegate.power.calculator.CombatValue;
import games.strategy.triplea.delegate.power.calculator.PowerStrengthAndRolls;
import games.strategy.triplea.delegate.power.calculator.TotalPowerAndTotalRolls;
import games.strategy.triplea.formatter.MyFormatter;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Used to store information about a dice roll.
 *
 * <p># of rolls at 5, at 4, etc.
 *
 * <p>Externalizable so we can efficiently write out our dice as ints rather than as full objects.
 */
public class DiceRoll implements Externalizable {
  private static final long serialVersionUID = -1167204061937566271L;
  private List<Die> rolls;
  // this does not need to match the Die with isHit true since for low luck we get many hits with
  // few dice
  private int hits;
  private double expectedHits;

  /**
   * Initializes a new instance of the DiceRoll class.
   *
   * @param dice the dice, 0 based
   * @param hits the number of hits
   * @param rollAt what we roll at, [0,Constants.MAX_DICE]
   * @param hitOnlyIfEquals Do we get a hit only if we are equals, or do we hit when we are equal or
   *     less than for example a 5 is a hit when rolling at 6 for equal and less than, but is not
   *     for equals.
   */
  public DiceRoll(
      final int[] dice, final int hits, final int rollAt, final boolean hitOnlyIfEquals) {
    this.hits = hits;
    expectedHits = 0;
    rolls =
        Arrays.stream(dice)
            .mapToObj(
                element -> {
                  final boolean hit = hitOnlyIfEquals ? (rollAt == element) : element <= rollAt;
                  return new Die(element, rollAt, hit ? DieType.HIT : DieType.MISS);
                })
            .collect(Collectors.toList());
  }

  // only for externalizable
  public DiceRoll() {}

  private DiceRoll(final List<Die> dice, final int hits, final double expectedHits) {
    rolls = new ArrayList<>(dice);
    this.hits = hits;
    this.expectedHits = expectedHits;
  }

  /**
   * Used to roll AA for battles, SBR, and fly over.
   *
   * @param validTargets Units that are being fired at
   * @param aaUnits Units that are firing. There must be at least one unit.
   */
  public static DiceRoll rollAa(
      final Collection<Unit> validTargets,
      final Collection<Unit> aaUnits,
      final IDelegateBridge bridge,
      final Territory battleSite,
      final CombatValue combatValueCalculator) {

    final String typeAa = UnitAttachment.get(aaUnits.iterator().next().getType()).getTypeAa();
    final GamePlayer player = aaUnits.iterator().next().getOwner();
    final String annotation =
        player.getName() + " roll " + typeAa + " dice in " + battleSite.getName();

    final AaPowerStrengthAndRolls unitPowerAndRollsMap =
        AaPowerStrengthAndRolls.build(aaUnits, validTargets.size(), combatValueCalculator);

    final DiceRoll diceRoll;
    if (Properties.getLowLuck(bridge.getData().getProperties())
        || Properties.getLowLuckAaOnly(bridge.getData().getProperties())) {
      diceRoll = rollDiceLowLuck(unitPowerAndRollsMap, player, bridge, annotation);
    } else {
      diceRoll = rollDiceNormal(unitPowerAndRollsMap, player, bridge, annotation);
    }

    bridge
        .getHistoryWriter()
        .addChildToEvent(annotation + " : " + MyFormatter.asDice(diceRoll), diceRoll);
    return diceRoll;
  }

  /**
   * Roll n-sided dice.
   *
   * @param annotation 0 based, add 1 to get actual die roll
   */
  public static DiceRoll rollNDice(
      final IDelegateBridge bridge,
      final int rollCount,
      final int sides,
      final GamePlayer playerRolling,
      final DiceType diceType,
      final String annotation) {
    if (rollCount == 0) {
      return new DiceRoll(new ArrayList<>(), 0, 0);
    }
    final int[] random = bridge.getRandom(sides, rollCount, playerRolling, diceType, annotation);
    final List<Die> dice = new ArrayList<>();
    for (int i = 0; i < rollCount; i++) {
      dice.add(new Die(random[i], 1, DieType.IGNORED));
    }
    return new DiceRoll(dice, rollCount, rollCount);
  }

  /**
   * Used to roll dice for attackers and defenders in battles.
   *
   * @param units - units that could potentially be rolling
   * @param player - that will be rolling the dice
   * @param bridge - delegate bridge
   * @param annotation - description of the battle being rolled for
   * @return DiceRoll result which includes total hits and dice that were rolled
   */
  public static DiceRoll rollDice(
      final Collection<Unit> units,
      final GamePlayer player,
      final IDelegateBridge bridge,
      final String annotation,
      final CombatValue combatValueCalculator) {

    final DiceRoll diceRoll;
    final PowerStrengthAndRolls unitPowerAndRollsMap =
        PowerStrengthAndRolls.build(units, combatValueCalculator);
    if (Properties.getLowLuck(bridge.getData().getProperties())) {
      diceRoll = rollDiceLowLuck(unitPowerAndRollsMap, player, bridge, annotation);
    } else {
      diceRoll = rollDiceNormal(unitPowerAndRollsMap, player, bridge, annotation);
    }

    bridge
        .getHistoryWriter()
        .addChildToEvent(annotation + " : " + MyFormatter.asDice(diceRoll), diceRoll);
    return diceRoll;
  }

  /** Roll dice for units using low luck rules. */
  private static DiceRoll rollDiceLowLuck(
      final TotalPowerAndTotalRolls totalPowerAndTotalRolls,
      final GamePlayer player,
      final IDelegateBridge bridge,
      final String annotation) {

    final int power = totalPowerAndTotalRolls.calculateTotalPower();
    if (power == 0) {
      return new DiceRoll(List.of(), 0, 0);
    }

    // Roll dice for the fractional part of the dice
    final int diceSides = totalPowerAndTotalRolls.getDiceSides();
    int hitCount = power / diceSides;
    final List<Die> dice = new ArrayList<>();
    final int rollFor = power % diceSides;
    if (rollFor > 0) {
      final int[] random = bridge.getRandom(diceSides, 1, player, DiceType.COMBAT, annotation);
      // Zero based
      final boolean hit = rollFor > random[0];
      if (hit) {
        hitCount++;
      }
      dice.add(new Die(random[0], rollFor, hit ? DieType.HIT : DieType.MISS));
    }

    // Create DiceRoll object
    final double expectedHits = ((double) power) / diceSides;

    return new DiceRoll(dice, hitCount, expectedHits);
  }

  /** Roll dice for units per normal rules. */
  private static DiceRoll rollDiceNormal(
      final TotalPowerAndTotalRolls totalPowerAndTotalRolls,
      final GamePlayer player,
      final IDelegateBridge bridge,
      final String annotation) {

    final int rollCount = totalPowerAndTotalRolls.calculateTotalRolls();
    if (rollCount == 0) {
      return new DiceRoll(new ArrayList<>(), 0, 0);
    }

    final int diceSides = totalPowerAndTotalRolls.getDiceSides();
    final int[] random =
        bridge.getRandom(diceSides, rollCount, player, DiceType.COMBAT, annotation);
    final List<Die> dice = totalPowerAndTotalRolls.getDiceHits(random);
    final int hitCount = (int) dice.stream().filter(die -> die.getType() == DieType.HIT).count();

    final int totalPower = totalPowerAndTotalRolls.calculateTotalPower();
    final double expectedHits = ((double) totalPower) / diceSides;

    return new DiceRoll(dice, hitCount, expectedHits);
  }

  /**
   * Parses the player name from the given annotation that has been produced by getAnnotation().
   *
   * @param annotation The annotation string.
   * @return The player's name.
   */
  public static String getPlayerNameFromAnnotation(final String annotation) {
    // This parses the "Germans roll dice for " format produced by getAnnotation() below.
    return annotation.split(" ", 2)[0];
  }

  public static String getAnnotation(
      final Collection<Unit> units,
      final GamePlayer player,
      final Territory territory,
      final int battleRound) {
    final StringBuilder buffer = new StringBuilder(80);
    // Note: This pattern is parsed when loading saved games to restore dice stats to get the player
    // name via the
    // getPlayerNameFromAnnotation() function above. When changing this format, update
    // getPlayerNameFromAnnotation(),
    // preferably in a way that is backwards compatible (can parse previous formats too).
    buffer
        .append(player.getName())
        .append(" roll dice for ")
        .append(MyFormatter.unitsToTextNoOwner(units))
        .append(" in ")
        .append(territory.getName())
        .append(", round ")
        .append(battleRound + 1);
    return buffer.toString();
  }

  public int getHits() {
    return hits;
  }

  public double getExpectedHits() {
    return expectedHits;
  }

  /**
   * Returns all rolls that are equal to the specified value.
   *
   * @param rollAt the strength of the roll, eg infantry roll at 2, expecting a number in [1,6]
   */
  public List<Die> getRolls(final int rollAt) {
    return rolls.stream() //
        .filter(die -> die.getRolledAt() == rollAt)
        .collect(Collectors.toList());
  }

  public int size() {
    return rolls.size();
  }

  public boolean isEmpty() {
    return rolls.isEmpty();
  }

  public Die getDie(final int index) {
    return rolls.get(index);
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    final int[] dice = new int[rolls.size()];
    for (int i = 0; i < rolls.size(); i++) {
      dice[i] = rolls.get(i).getCompressedValue();
    }
    out.writeObject(dice);
    out.writeInt(hits);
    out.writeDouble(expectedHits);
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    final int[] dice = (int[]) in.readObject();
    rolls = new ArrayList<>(dice.length);
    for (final int element : dice) {
      rolls.add(Die.getFromWriteValue(element));
    }
    hits = in.readInt();
    expectedHits = in.readDouble();
  }

  @Override
  public String toString() {
    return "DiceRoll dice:" + rolls + " hits:" + hits + " expectedHits:" + expectedHits;
  }
}
