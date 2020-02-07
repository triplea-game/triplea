package games.strategy.engine.chat;

import org.triplea.domain.data.UserName;
import org.triplea.game.chat.ChatModel;
import org.triplea.http.client.lobby.chat.messages.server.ChatMessage;

/** Headless version of ChatPanel. */
public class HeadlessChat implements ChatMessageListener, ChatModel {
  private final Chat chat;

  public HeadlessChat(final Chat chat) {
    this.chat = chat;
    chat.addChatListener(this);
  }

  @Override
  public Chat getChat() {
    return chat;
  }

  @Override
  public void eventReceived(final String eventText) {}

  @Override
  public void messageReceived(final ChatMessage chatMessage) {}

  @Override
  public void slapped(final String message, final UserName from) {
    messageReceived(new ChatMessage(from, message));
  }

  @Override
  public void slap(final String message) {}

  @Override
  public void playerJoined(final String message) {}

  @Override
  public void playerLeft(final String message) {}
}
