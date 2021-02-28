package org.triplea.modules.moderation.chat.history;

import java.util.List;
import java.util.function.Function;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.http.HttpController;
import org.triplea.http.client.lobby.moderator.ChatHistoryMessage;
import org.triplea.http.client.lobby.moderator.ModeratorChatClient;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@RolesAllowed(UserRole.MODERATOR)
public class GameChatHistoryController extends HttpController {

  private final Function<String, List<ChatHistoryMessage>> fetchChatHistoryAction;

  public static GameChatHistoryController build(final Jdbi jdbi) {
    return new GameChatHistoryController(FetchGameChatHistoryModule.build(jdbi));
  }

  @POST
  @Path(ModeratorChatClient.FETCH_GAME_CHAT_HISTORY)
  public List<ChatHistoryMessage> fetchGameChatHistory(final String gameId) {
    return fetchChatHistoryAction.apply(gameId);
  }
}
