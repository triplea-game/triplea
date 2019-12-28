package games.strategy.engine.chat;

import com.google.common.base.Ascii;
import org.triplea.domain.data.UserName;
import org.triplea.game.chat.ChatModel;
import org.triplea.http.client.lobby.chat.messages.server.ChatMessage;
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

  @Override
  public void eventReceived(final String eventText) {
    allText.append(eventText);
  }

  @Override
  public void messageReceived(final ChatMessage chatMessage) {
    trimLengthIfNecessary();

    final String message = Ascii.truncate(chatMessage.getMessage(), 200, "...");
    final String fullMessage =
        String.format(
            "(%s) %s: %s\n", TimeManager.getLocalizedTime(), chatMessage.getFrom(), message);
    allText.append(fullMessage);
  }

  @Override
  public void slapped(final String message, final UserName from) {
    messageReceived(new ChatMessage(from, message));
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
