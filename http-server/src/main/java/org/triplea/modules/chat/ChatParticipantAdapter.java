package org.triplea.modules.chat;

import java.util.function.BiFunction;
import javax.websocket.Session;
import org.triplea.db.dao.api.key.ApiKeyLookupRecord;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.modules.chat.Chatters.ChatterSession;

class ChatParticipantAdapter implements BiFunction<Session, ApiKeyLookupRecord, ChatterSession> {

  @Override
  public ChatterSession apply(final Session session, final ApiKeyLookupRecord apiKeyLookupRecord) {
    return ChatterSession.builder()
        .apiKeyId(apiKeyLookupRecord.getApiKeyId())
        .chatParticipant(buildChatParticipant(apiKeyLookupRecord))
        .session(session)
        .build();
  }

  private ChatParticipant buildChatParticipant(final ApiKeyLookupRecord apiKeyLookupRecord) {
    return ChatParticipant.builder()
        .userName(apiKeyLookupRecord.getUsername())
        .isModerator(
            apiKeyLookupRecord.getRole().equals(UserRole.ADMIN)
                || apiKeyLookupRecord.getRole().equals(UserRole.MODERATOR))
        .playerChatId(apiKeyLookupRecord.getPlayerChatId())
        .build();
  }
}
