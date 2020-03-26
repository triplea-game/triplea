package org.triplea.modules.user.account.update;

import com.google.common.base.Preconditions;
import io.dropwizard.auth.Auth;
import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.data.UserRole;
import org.triplea.http.HttpController;
import org.triplea.http.client.lobby.user.account.FetchEmailResponse;
import org.triplea.http.client.lobby.user.account.UserAccountClient;
import org.triplea.java.ArgChecker;
import org.triplea.modules.access.authentication.AuthenticatedUser;

/** Controller providing endpoints for user account management. */
@Builder
public class UpdateAccountController extends HttpController {
  @Nonnull private final UpdateAccountService userAccountService;

  /** Instantiates controller with dependencies. */
  public static UpdateAccountController build(final Jdbi jdbi) {
    return UpdateAccountController.builder()
        .userAccountService(UpdateAccountService.build(jdbi))
        .build();
  }

  @POST
  @Path(UserAccountClient.CHANGE_PASSWORD_PATH)
  @RolesAllowed(UserRole.PLAYER)
  public Response changePassword(
      @Auth final AuthenticatedUser authenticatedUser, final String newPassword) {
    ArgChecker.checkNotEmpty(newPassword);
    Preconditions.checkArgument(authenticatedUser.getUserIdOrThrow() > 0);

    userAccountService.changePassword(authenticatedUser.getUserIdOrThrow(), newPassword);
    return Response.ok().build();
  }

  @GET
  @Path(UserAccountClient.FETCH_EMAIL_PATH)
  @RolesAllowed(UserRole.PLAYER)
  public FetchEmailResponse fetchEmail(@Auth final AuthenticatedUser authenticatedUser) {
    Preconditions.checkArgument(authenticatedUser.getUserIdOrThrow() > 0);

    return new FetchEmailResponse(
        userAccountService.fetchEmail(authenticatedUser.getUserIdOrThrow()));
  }

  @POST
  @Path(UserAccountClient.CHANGE_EMAIL_PATH)
  @RolesAllowed(UserRole.PLAYER)
  public Response changeEmail(
      @Auth final AuthenticatedUser authenticatedUser, final String newEmail) {
    ArgChecker.checkNotEmpty(newEmail);
    Preconditions.checkArgument(authenticatedUser.getUserIdOrThrow() > 0);

    userAccountService.changeEmail(authenticatedUser.getUserIdOrThrow(), newEmail);
    return Response.ok().build();
  }
}
