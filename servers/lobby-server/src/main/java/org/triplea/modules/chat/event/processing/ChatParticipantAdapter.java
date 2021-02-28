package org.triplea.modules.chat.event.processing;

import java.util.function.BiFunction;
import org.triplea.db.dao.api.key.PlayerApiKeyLookupRecord;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.modules.chat.ChatterSession;
import org.triplea.web.socket.WebSocketSession;

class ChatParticipantAdapter
    implements BiFunction<WebSocketSession, PlayerApiKeyLookupRecord, ChatterSession> {

  @Override
  public ChatterSession apply(
      final WebSocketSession session, final PlayerApiKeyLookupRecord apiKeyLookupRecord) {
    return ChatterSession.builder()
        .apiKeyId(apiKeyLookupRecord.getApiKeyId())
        .chatParticipant(buildChatParticipant(apiKeyLookupRecord))
        .session(session)
        .ip(session.getRemoteAddress())
        .build();
  }

  private ChatParticipant buildChatParticipant(final PlayerApiKeyLookupRecord apiKeyLookupRecord) {
    return ChatParticipant.builder()
        .userName(apiKeyLookupRecord.getUsername())
        .isModerator(
            apiKeyLookupRecord.getUserRole().equals(UserRole.ADMIN)
                || apiKeyLookupRecord.getUserRole().equals(UserRole.MODERATOR))
        .playerChatId(apiKeyLookupRecord.getPlayerChatId())
        .build();
  }
}
