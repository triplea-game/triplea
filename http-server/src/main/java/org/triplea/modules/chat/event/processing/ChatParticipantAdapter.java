package org.triplea.modules.chat.event.processing;

import java.util.function.BiFunction;
import javax.websocket.Session;
import org.triplea.db.dao.api.key.PlayerApiKeyLookupRecord;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.modules.chat.ChatterSession;
import org.triplea.web.socket.InetExtractor;

class ChatParticipantAdapter
    implements BiFunction<Session, PlayerApiKeyLookupRecord, ChatterSession> {

  @Override
  public ChatterSession apply(
      final Session session, final PlayerApiKeyLookupRecord apiKeyLookupRecord) {
    return ChatterSession.builder()
        .apiKeyId(apiKeyLookupRecord.getApiKeyId())
        .chatParticipant(buildChatParticipant(apiKeyLookupRecord))
        .session(session)
        .ip(InetExtractor.extract(session.getUserProperties()))
        .build();
  }

  private ChatParticipant buildChatParticipant(final PlayerApiKeyLookupRecord apiKeyLookupRecord) {
    return ChatParticipant.builder()
        .userName(apiKeyLookupRecord.getUsername())
        .isModerator(
            apiKeyLookupRecord.getRole().equals(UserRole.ADMIN)
                || apiKeyLookupRecord.getRole().equals(UserRole.MODERATOR))
        .playerChatId(apiKeyLookupRecord.getPlayerChatId())
        .build();
  }
}
