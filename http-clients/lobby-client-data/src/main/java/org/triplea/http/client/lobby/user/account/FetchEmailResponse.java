package org.triplea.http.client.lobby.user.account;

import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString
@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class FetchEmailResponse {
  /** User's email as found in database. */
  @Nonnull private final String userEmail;
}
