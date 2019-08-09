package org.triplea.http.client.moderator.toolbox.moderator.management;

import java.time.Instant;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Data object meant for JSON encoding and transport between server and client. Contains data to
 * list the moderators.
 */
@Builder
@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class ModeratorInfo {
  private final String name;
  @Nullable private final Instant lastLogin;
}
