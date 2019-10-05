package org.triplea.http.client.lobby.login;

import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Represents data that would be uploaded to a server. */
@Getter
@Builder
@EqualsAndHashCode
// TODO: Project#12 - move to package org.triplea.http.client.account.login
public class RegisteredUserLoginRequest {
  @Nonnull private final String name;
  @Nonnull private final String password;
}
