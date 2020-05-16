package org.triplea.modules.moderation.mute.user;

import com.google.common.base.Preconditions;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import lombok.Builder;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.HttpController;
import org.triplea.http.client.lobby.moderator.ModeratorChatClient;
import org.triplea.http.client.lobby.moderator.MuteUserRequest;
import org.triplea.modules.chat.Chatters;

@Builder
@RolesAllowed(UserRole.MODERATOR)
public class MuteUserController extends HttpController {

  private final Chatters chatters;

  public static MuteUserController build(final Chatters chatters) {
    return new MuteUserController(chatters);
  }

  @POST
  @Path(ModeratorChatClient.MUTE_USER)
  public Response muteUser(final MuteUserRequest muteUserRequest) {
    Preconditions.checkArgument(muteUserRequest != null);
    Preconditions.checkArgument(muteUserRequest.getPlayerChatId() != null);
    Preconditions.checkArgument(muteUserRequest.getMinutes() > 0);

    chatters.mutePlayer(
        PlayerChatId.of(muteUserRequest.getPlayerChatId()), muteUserRequest.getMinutes());
    return Response.ok().build();
  }
}
