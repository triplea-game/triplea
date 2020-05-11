package org.triplea.http.client.lobby.moderator.toolbox.banned.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/** JSON data object to pass user ban information from the server to the front-end. */
@Builder
@Getter
@EqualsAndHashCode
@AllArgsConstructor
@ToString
public class UserBanData {
  private final String banId;
  private final String username;
  private final Long banDate;
  private final String ip;
  private final String hashedMac;
  private final Long banExpiry;
}
