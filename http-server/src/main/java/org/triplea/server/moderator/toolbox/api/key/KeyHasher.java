package org.triplea.server.moderator.toolbox.api.key;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class to provide hashing functions for moderator API key.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KeyHasher {

  public static String applyHash(final String apiKey, final String password) {
    Preconditions.checkNotNull(apiKey);
    Preconditions.checkNotNull(password);
    return applyHash(apiKey + password);
  }

  public static String applyHash(final String valueToHash) {
    Preconditions.checkNotNull(valueToHash);
    Preconditions.checkArgument(!valueToHash.isEmpty());
    // TODO: Use bcrypt hash
    return Hashing.sha512().hashString(valueToHash, Charsets.UTF_8).toString();
  }
}
