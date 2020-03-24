package org.triplea.modules.user.account.create;

import com.google.common.base.Preconditions;
import es.moki.ratelimij.dropwizard.annotation.Rate;
import es.moki.ratelimij.dropwizard.annotation.RateLimited;
import es.moki.ratelimij.dropwizard.filter.KeyPart;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import lombok.Builder;
import org.triplea.http.HttpController;
import org.triplea.http.client.lobby.login.CreateAccountRequest;
import org.triplea.http.client.lobby.login.CreateAccountResponse;
import org.triplea.http.client.lobby.login.LobbyLoginClient;

@Builder
public class CreateAccountController extends HttpController {

  @Nonnull private final Function<CreateAccountRequest, CreateAccountResponse> createAccountModule;

  @POST
  @Path(LobbyLoginClient.CREATE_ACCOUNT)
  @RateLimited(
      keys = {KeyPart.IP},
      rates = {@Rate(limit = 15, duration = 1, timeUnit = TimeUnit.HOURS)})
  public CreateAccountResponse createAccount(final CreateAccountRequest createAccountRequest) {
    Preconditions.checkNotNull(createAccountRequest);
    return createAccountModule.apply(createAccountRequest);
  }
}
