package org.triplea.modules.moderation.mute.user;

import com.google.common.base.Preconditions;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import lombok.Builder;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.http.HttpController;
import org.triplea.http.client.lobby.moderator.ModeratorChatClient;
import org.triplea.http.client.lobby.moderator.MuteUserRequest;

@Builder
@RolesAllowed(UserRole.MODERATOR)
public class MuteUserController extends HttpController {

  public static MuteUserController build() {
    return new MuteUserController();
  }

  @POST
  @Path(ModeratorChatClient.MUTE_USER)
  public Response muteUser(final MuteUserRequest muteUserRequest) {
    Preconditions.checkArgument(muteUserRequest != null);
    Preconditions.checkArgument(muteUserRequest.getPlayerChatId() != null);
    Preconditions.checkArgument(muteUserRequest.getMinutes() > 0);

    // WIP: implement
    return Response.ok().build();
  }
}
