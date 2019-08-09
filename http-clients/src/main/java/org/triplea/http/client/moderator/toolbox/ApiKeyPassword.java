package org.triplea.http.client.moderator.toolbox;

import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;

/** Simple immutable data object for holding the pair value of api key and its password. */
@Builder
@Getter
public class ApiKeyPassword {
  @Nonnull private final String apiKey;
  @Nonnull private final String password;
}
