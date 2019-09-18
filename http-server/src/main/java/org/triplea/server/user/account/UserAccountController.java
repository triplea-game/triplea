package org.triplea.server.user.account;

import com.google.common.base.Preconditions;
import io.dropwizard.auth.Auth;
import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import lombok.Builder;
import org.triplea.http.client.lobby.user.account.FetchEmailResponse;
import org.triplea.http.client.lobby.user.account.UserAccountClient;
import org.triplea.java.ArgChecker;
import org.triplea.lobby.server.db.data.UserRole;
import org.triplea.server.access.AuthenticatedUser;

/** Controller providing endpoints for user account management. */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Builder
public class UserAccountController {

  @Nonnull private final UserAccountService userAccountService;

  @POST
  @Path(UserAccountClient.CHANGE_PASSWORD_PATH)
  @RolesAllowed(UserRole.PLAYER)
  public Response changePassword(
      @Auth final AuthenticatedUser authenticatedUser, final String newPassword) {
    ArgChecker.checkNotEmpty(newPassword);
    // TODO: Project#12 use 'getUserIdOrThrow'
    Preconditions.checkArgument(authenticatedUser.getUserId() > 0);

    userAccountService.changePassword(authenticatedUser.getUserId(), newPassword);
    return Response.ok().build();
  }

  @GET
  @Path(UserAccountClient.FETCH_EMAIL_PATH)
  @RolesAllowed(UserRole.PLAYER)
  public FetchEmailResponse fetchEmail(@Auth final AuthenticatedUser authenticatedUser) {
    Preconditions.checkArgument(authenticatedUser.getUserId() > 0);

    final String email = userAccountService.fetchEmail(authenticatedUser.getUserId());
    return new FetchEmailResponse(email);
  }

  @POST
  @Path(UserAccountClient.CHANGE_EMAIL_PATH)
  @RolesAllowed(UserRole.PLAYER)
  public Response changeEmail(
      @Auth final AuthenticatedUser authenticatedUser, final String newEmail) {
    ArgChecker.checkNotEmpty(newEmail);
    Preconditions.checkArgument(authenticatedUser.getUserId() > 0);

    userAccountService.changeEmail(authenticatedUser.getUserId(), newEmail);
    return Response.ok().build();
  }
}
