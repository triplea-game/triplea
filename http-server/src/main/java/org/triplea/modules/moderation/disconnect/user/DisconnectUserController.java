package org.triplea.modules.moderation.disconnect.user;

import com.google.common.base.Preconditions;
import es.moki.ratelimij.dropwizard.annotation.Rate;
import es.moki.ratelimij.dropwizard.annotation.RateLimited;
import es.moki.ratelimij.dropwizard.filter.KeyPart;
import io.dropwizard.auth.Auth;
import java.util.concurrent.TimeUnit;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.data.UserRole;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.HttpController;
import org.triplea.http.client.lobby.moderator.ModeratorChatClient;
import org.triplea.modules.access.authentication.AuthenticatedUser;
import org.triplea.modules.chat.Chatters;
import org.triplea.web.socket.WebSocketMessagingBus;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@RolesAllowed(UserRole.MODERATOR)
public class DisconnectUserController extends HttpController {

  private final DisconnectUserAction disconnectUserAction;

  public static DisconnectUserController build(
      final Jdbi jdbi, final Chatters chatters, final WebSocketMessagingBus playerConnections) {
    return new DisconnectUserController(
        DisconnectUserAction.build(jdbi, chatters, playerConnections));
  }

  @POST
  @Path(ModeratorChatClient.DISCONNECT_PLAYER_PATH)
  @RateLimited(
      keys = {KeyPart.IP},
      rates = {@Rate(limit = 5, duration = 1, timeUnit = TimeUnit.MINUTES)})
  public Response disconnectPlayer(
      @Auth final AuthenticatedUser authenticatedUser, final String playerIdToBan) {
    Preconditions.checkNotNull(playerIdToBan);

    final boolean removed =
        disconnectUserAction.disconnectPlayer(
            authenticatedUser.getUserIdOrThrow(), PlayerChatId.of(playerIdToBan));
    return Response.status(removed ? 200 : 400).build();
  }
}
