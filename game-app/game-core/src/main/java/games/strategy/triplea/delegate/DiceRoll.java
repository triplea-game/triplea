package games.strategy.triplea.delegate;

import com.google.common.primitives.Ints;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Die.DieType;
import games.strategy.triplea.formatter.MyFormatter;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import org.triplea.java.ChangeOnNextMajorRelease;

/**
 * Used to store information about a dice roll.
 *
 * <p># of rolls at 5, at 4, etc.
 *
 * <p>Externalizable so we can efficiently write out our dice as ints rather than as full objects.
 */
@Getter
@ChangeOnNextMajorRelease("This class should be moved to dice.calculator")
public class DiceRoll implements Externalizable {
  private static final long serialVersionUID = -1167204061937566271L;
  private List<Die> rolls;
  // this does not need to match the Die with isHit true since for low luck we get many hits with
  // few dice
  private int hits;
  private double expectedHits;
  // keep track of the randomness stats for the player
  private String playerName = null;

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
      final int[] dice,
      final int hits,
      final int rollAt,
      final boolean hitOnlyIfEquals,
      final String playerName) {
    this.hits = hits;
    this.playerName = playerName;
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

  @ChangeOnNextMajorRelease("This constructor should be made package visible once it is moved")
  public DiceRoll(
      final List<Die> dice, final int hits, final double expectedHits, final String playerName) {
    rolls = List.copyOf(dice);
    this.hits = hits;
    this.expectedHits = expectedHits;
    this.playerName = playerName;
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
    // Note: This pattern is parsed when loading saved games to restore dice stats to get the player
    // name via the getPlayerNameFromAnnotation() function above. When changing this format, update
    // getPlayerNameFromAnnotation(), preferably in a way that is backwards compatible (can parse
    // previous formats too).
    return player.getName()
        + " roll dice for "
        + MyFormatter.unitsToTextNoOwner(units)
        + " in "
        + territory.getName()
        + ", round "
        + (battleRound + 1);
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
    // add a marker to indicate that this is versioned
    out.writeInt(1);
    final int[] dice = new int[rolls.size()];
    for (int i = 0; i < rolls.size(); i++) {
      dice[i] = rolls.get(i).getCompressedValue();
    }
    out.writeObject(dice);
    out.writeInt(hits);
    out.writeDouble(expectedHits);
    out.writeObject(playerName);
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    final int serializedVersion = readSerializeVersion(in);

    final int[] dice = (int[]) in.readObject();
    rolls = Ints.asList(dice).stream().map(Die::getFromWriteValue).collect(Collectors.toList());
    hits = in.readInt();
    expectedHits = in.readDouble();
    playerName = serializedVersion == 1 ? (String) in.readObject() : null;
  }

  /** Read the serialized version to support backwards compatibility */
  private int readSerializeVersion(final ObjectInput in) {
    try {
      return in.readInt();
    } catch (final IOException ignored) {
      // the int doesn't exist in the stream so this is the initial version
      return 0;
    }
  }

  @Override
  public String toString() {
    return "DiceRoll dice:" + rolls + " hits:" + hits + " expectedHits:" + expectedHits;
  }
}
