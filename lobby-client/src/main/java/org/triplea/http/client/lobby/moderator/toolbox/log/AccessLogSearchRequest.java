package org.triplea.http.client.lobby.moderator.toolbox.log;

import java.util.Optional;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Represents search params from the moderator toolbox to query the access log table. */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccessLogSearchRequest {
  public static final AccessLogSearchRequest EMPTY_SEARCH =
      AccessLogSearchRequest.builder().ip("").systemId("").username("").build();

  private String ip;
  private String systemId;
  private String username;

  public String getIp() {
    return normalize(ip);
  }

  /**
   * If string is not specified or blank then we want to convert it to a wildcard. Otherwise we will
   * to trim off leading and trailing spaces.
   */
  private String normalize(final String input) {
    return Optional.ofNullable(input)
        .filter(Predicate.not(String::isBlank))
        .map(String::trim)
        .orElse("%");
  }

  public String getSystemId() {
    return normalize(systemId);
  }

  public String getUsername() {
    return normalize(username);
  }
}
