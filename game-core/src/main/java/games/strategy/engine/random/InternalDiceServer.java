package games.strategy.engine.random;

import java.io.ObjectStreamException;
import java.util.Objects;

import games.strategy.engine.framework.startup.ui.editors.DiceServerEditor;
import games.strategy.engine.framework.startup.ui.editors.EditorPanel;

/**
 * This is not actually a dice server, it just uses the normal TripleA PlainRandomSource for dice roll
 * This way your dice rolls are not registered anywhere, and you do not rely on any external web based service rolling
 * the dice.
 * Because DiceServers must be serializable read resolve must be implemented
 */
public class InternalDiceServer implements IRemoteDiceServer {
  private static final long serialVersionUID = -8369097763085658445L;
  private final transient IRandomSource _randomSource;

  public InternalDiceServer() {
    _randomSource = new PlainRandomSource();
  }

  @Override
  public EditorPanel getEditor() {
    return new DiceServerEditor(this);
  }

  @Override
  public String postRequest(final int max, final int numDice, final String subjectMessage, final String gameId,
      final String gameUuid) {
    // the interface is rather stupid, you have to return a string here, which is then passed back in getDice()
    final int[] ints = _randomSource.getRandom(max, numDice, "Internal Dice Server");
    final StringBuilder sb = new StringBuilder();
    for (final int i : ints) {
      sb.append(i).append(",");
    }
    final String intArrayString = sb.substring(0, sb.length() - 1);
    return intArrayString;
  }

  @Override
  public int[] getDice(final String string, final int count) {
    final String[] strArray = string.split(",");
    final int[] intArray = new int[strArray.length];
    for (int i = 0; i < strArray.length; i++) {
      intArray[i] = Integer.parseInt(strArray[i]);
    }
    return intArray;
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
  public void setToAddress(final String toAddress) {}

  @Override
  public String getCcAddress() {
    return null;
  }

  @Override
  public void setCcAddress(final String ccAddress) {}

  @Override
  public String getInfoText() {
    return "Uses the build in TripleA dice roller.\n"
        + "Dice are not logged, and no internet access is required.\n"
        + "It is technically possible (for a hacker) to modify the dice rolls.";
  }

  @Override
  public boolean sendsEmail() {
    return false;
  }

  /**
   * Dice servers has to be serializable, so we need to provide custom serialization since
   * PlainRandomSource is not serializable.
   *
   * @return a new InternalDiceServer
   * @throws ObjectStreamException
   *         should never occur (unless runtime exceptions is thrown from constructor)
   */
  public Object readResolve() {
    return new InternalDiceServer();
  }

  @Override
  public boolean supportsGameId() {
    return false;
  }

  @Override
  public void setGameId(final String gameId) {}

  @Override
  public String getGameId() {
    return null;
  }

  @Override
  public String getHelpText() {
    return "<html>No help</html>";
  }

  @Override
  public boolean equals(final Object other) {
    return (other != null) && getClass().equals(other.getClass());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getClass());
  }
}
