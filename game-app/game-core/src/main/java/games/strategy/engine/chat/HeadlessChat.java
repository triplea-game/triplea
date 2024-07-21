package games.strategy.engine.chat;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.domain.data.UserName;
import org.triplea.game.chat.ChatModel;

/** Headless version of ChatPanel. */
@Slf4j
public class HeadlessChat implements ChatMessageListener, ChatModel {
  @Getter(onMethod_ = @Override)
  private final Chat chat;

  public HeadlessChat(final Chat chat) {
    this.chat = chat;
  }

  @Override
  public void eventReceived(final String eventText) {}

  @Override
  public void messageReceived(final UserName fromPlayer, final String chatMessage) {
    log.info(String.format("Chat Message {Player: %s, Message: %s}", fromPlayer, chatMessage));
  }

  @Override
  public void slapped(final UserName from) {}

  @Override
  public void playerJoined(final String message) {}

  @Override
  public void playerLeft(final String message) {}

  @Override
  public void cancel() {}
}
