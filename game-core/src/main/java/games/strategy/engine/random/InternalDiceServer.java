package games.strategy.engine.random;

import com.google.common.base.Splitter;

/**
 * This is not actually a dice server, it just uses the normal TripleA PlainRandomSource for dice roll
 * This way your dice rolls are not registered anywhere, and you do not rely on any external web based service rolling
 * the dice.
 * Because DiceServers must be serializable read resolve must be implemented
 */
public final class InternalDiceServer implements IRemoteDiceServer {
  private static final char DICE_SEPARATOR = ',';

  private final transient IRandomSource randomSource = new PlainRandomSource();

  @Override
  public String postRequest(final int max, final int numDice, final String subjectMessage, final String gameId) {
    // the interface is rather stupid, you have to return a string here, which is then passed back in getDice()
    final int[] ints = randomSource.getRandom(max, numDice, "Internal Dice Server");
    final StringBuilder sb = new StringBuilder();
    for (final int i : ints) {
      sb.append(i).append(DICE_SEPARATOR);
    }
    return sb.substring(0, sb.length() - 1);
  }

  @Override
  public int[] getDice(final String string, final int count) {
    return Splitter.on(DICE_SEPARATOR).splitToList(string).stream()
        .mapToInt(Integer::parseInt)
        .toArray();
  }

  @Override
  public String getDisplayName() {
    return "Internal Dice Roller";
  }

  @Override
  public String getToAddress() {
    return null;
  }

  @Override
  public String getCcAddress() {
    return null;
  }

  @Override
  public String getGameId() {
    return null;
  }
}
