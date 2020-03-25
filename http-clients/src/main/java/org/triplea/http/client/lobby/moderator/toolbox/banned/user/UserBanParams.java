package org.triplea.http.client.lobby.moderator.toolbox.banned.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data object to be encoded to and from JSON. Contains the parameters the backend would need to add
 * a new user ban entry.
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserBanParams {
  private String ip;
  private String username;
  private String systemId;
  private long minutesToBan;
}
