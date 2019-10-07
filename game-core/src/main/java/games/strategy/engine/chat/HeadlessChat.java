package games.strategy.engine.chat;

import com.google.common.base.Ascii;
import games.strategy.engine.lobby.PlayerName;
import org.triplea.game.chat.ChatModel;
import org.triplea.java.TimeManager;

/** Headless version of ChatPanel. */
public class HeadlessChat implements ChatMessageListener, ChatModel {
  // roughly 1000 chat messages
  private static final int MAX_LENGTH = 1000 * 200;
  private final Chat chat;
  private final StringBuilder allText = new StringBuilder();

  public HeadlessChat(final Chat chat) {
    this.chat = chat;
    chat.addChatListener(this);
  }

  @Override
  public String toString() {
    return allText.toString();
  }

  @Override
  public String getAllText() {
    return allText.toString();
  }

  @Override
  public Chat getChat() {
    return chat;
  }

  /** thread safe. */
  @Override
  public void messageReceived(final String originalMessage, final PlayerName from) {
    trimLengthIfNecessary();

    final String message = Ascii.truncate(originalMessage, 200, "...");
    final String fullMessage =
        String.format("(%s) %s: %s\n", TimeManager.getLocalizedTime(), from, message);
    allText.append(fullMessage);
  }

  /** thread safe. */
  @Override
  public void slap(final String message, final PlayerName from) {
    messageReceived(message, from);
  }

  @Override
  public void slap(final String message) {
    addGenericMessage(message);
  }

  @Override
  public void playerJoined(final String message) {
    addGenericMessage(message);
  }

  @Override
  public void playerLeft(final String message) {
    addGenericMessage(message);
  }

  private void addGenericMessage(final String message) {
    trimLengthIfNecessary();
    allText.append("--- ").append(message).append(" ---\n");
  }

  private void trimLengthIfNecessary() {
    if (allText.length() > MAX_LENGTH) {
      allText.delete(0, MAX_LENGTH / 2);
    }
  }
}
