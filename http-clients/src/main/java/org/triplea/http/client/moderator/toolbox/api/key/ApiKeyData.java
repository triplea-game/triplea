package org.triplea.http.client.moderator.toolbox.api.key;

import java.time.Instant;

import javax.annotation.Nullable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * JSON transport data object, meant to be encoded to and from JSON.
 * Contains information about a users API keys. The payload does not
 * contain the actual key value, instead an identifier that the backend
 * can use to recognize and delete keys.
 */
@Builder
@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class ApiKeyData {
  private final String publicId;
  @Nullable
  private final Instant lastUsed;
  private final String lastUsedIp;
}
