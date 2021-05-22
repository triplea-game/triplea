package org.triplea.modules.moderation.chat.history;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.moderator.chat.history.ChatHistoryRecord;
import org.triplea.db.dao.moderator.chat.history.GameChatHistoryDao;
import org.triplea.http.client.lobby.moderator.ChatHistoryMessage;

@AllArgsConstructor
class FetchGameChatHistoryModule implements Function<String, List<ChatHistoryMessage>> {
  private final GameChatHistoryDao gameChatHistoryDao;

  static FetchGameChatHistoryModule build(final Jdbi jdbi) {
    return new FetchGameChatHistoryModule(jdbi.onDemand(GameChatHistoryDao.class));
  }

  @Override
  public List<ChatHistoryMessage> apply(final String gameId) {
    return gameChatHistoryDao.getChatHistory(gameId).stream()
        .map(ChatHistoryRecord::toChatHistoryMessage)
        .collect(Collectors.toList());
  }
}
