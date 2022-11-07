package org.triplea.http.client.lobby.moderator.toolbox.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * JSON transport data object, meant to be encoded to and from JSON. Represents rows of the lobby
 * access table.
 */
@Builder
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class AccessLogData {
  private final Long accessDate;
  private final String username;
  private final String ip;
  private final String systemId;
  private final boolean registered;
}
