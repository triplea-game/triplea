package games.strategy.engine.chat;

import com.google.common.base.Ascii;
import games.strategy.engine.chat.Chat.ChatSoundProfile;
import games.strategy.engine.lobby.PlayerName;
import games.strategy.net.Messengers;
import java.util.Collection;
import org.triplea.game.chat.ChatModel;
import org.triplea.java.TimeManager;

/** Headless version of ChatPanel. */
public class HeadlessChat implements IChatListener, ChatModel {
  // roughly 1000 chat messages
  private static final int MAX_LENGTH = 1000 * 200;
  private Chat chat;
  private final StringBuilder allText = new StringBuilder();

  public HeadlessChat(
      final Messengers messengers, final String chatName, final ChatSoundProfile chatSoundProfile) {
    this.chat = new Chat(messengers, chatName, chatSoundProfile);
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
  public void updatePlayerList(final Collection<ChatParticipant> players) {}

  /** thread safe. */
  @Override
  public void addMessage(final String originalMessage, final PlayerName from) {
    trimLengthIfNecessary();

    final String message = Ascii.truncate(originalMessage, 200, "...");
    final String fullMessage =
        String.format("(%s) %s: %s\n", TimeManager.getLocalizedTime(), from, message);
    allText.append(fullMessage);
  }

  /** thread safe. */
  @Override
  public void addMessageWithSound(final String message, final PlayerName from, final String sound) {
    addMessage(message, from);
  }

  @Override
  public void addStatusMessage(final String message) {
    trimLengthIfNecessary();
    allText.append("--- ").append(message).append(" ---\n");
  }

  private void trimLengthIfNecessary() {
    if (allText.length() > MAX_LENGTH) {
      allText.delete(0, MAX_LENGTH / 2);
    }
  }
}
