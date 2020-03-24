package org.triplea.modules.chat.moderation;

import com.google.common.base.Preconditions;
import es.moki.ratelimij.dropwizard.annotation.Rate;
import es.moki.ratelimij.dropwizard.annotation.RateLimited;
import es.moki.ratelimij.dropwizard.filter.KeyPart;
import io.dropwizard.auth.Auth;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import lombok.Builder;
import org.triplea.db.data.UserRole;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.HttpController;
import org.triplea.http.client.lobby.moderator.BanPlayerRequest;
import org.triplea.http.client.lobby.moderator.ModeratorChatClient;
import org.triplea.modules.access.authentication.AuthenticatedUser;

@Builder
@RolesAllowed(UserRole.MODERATOR)
public class ModeratorChatController extends HttpController {
  @Nonnull private ModeratorChatService moderatorChatService;

  @POST
  @Path(ModeratorChatClient.BAN_PLAYER_PATH)
  @RateLimited(
      keys = {KeyPart.IP},
      rates = {@Rate(limit = 5, duration = 1, timeUnit = TimeUnit.MINUTES)})
  public Response banPlayer(
      @Auth final AuthenticatedUser authenticatedUser, final BanPlayerRequest banPlayerRequest) {
    Preconditions.checkNotNull(banPlayerRequest);
    Preconditions.checkNotNull(banPlayerRequest.getPlayerChatId());
    Preconditions.checkArgument(banPlayerRequest.getBanMinutes() > 0);

    moderatorChatService.banPlayer(authenticatedUser.getUserIdOrThrow(), banPlayerRequest);

    return Response.ok().build();
  }

  @POST
  @Path(ModeratorChatClient.DISCONNECT_PLAYER_PATH)
  @RateLimited(
      keys = {KeyPart.IP},
      rates = {@Rate(limit = 5, duration = 1, timeUnit = TimeUnit.MINUTES)})
  public Response disconnectPlayer(
      @Auth final AuthenticatedUser authenticatedUser, final String playerIdToBan) {
    Preconditions.checkNotNull(playerIdToBan);

    moderatorChatService.disconnectPlayer(
        authenticatedUser.getUserIdOrThrow(), PlayerChatId.of(playerIdToBan));

    return Response.ok().build();
  }
}
