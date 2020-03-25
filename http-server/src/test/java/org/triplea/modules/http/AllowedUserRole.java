package org.triplea.modules.http;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import lombok.Getter;
import org.triplea.domain.data.ApiKey;

/**
 * Mapping of an allowed user role to a set of keys that should be granted access for that role.
 * Also stores the complement, keys that should be denied access for a given role.
 */
@SuppressWarnings("ImmutableEnumChecker")
public enum AllowedUserRole {
  ADMIN(KeyValues.ADMIN, KeyValues.MODERATOR, KeyValues.HOST),
  MODERATOR(KeyValues.MODERATOR, KeyValues.PLAYER, KeyValues.HOST),
  PLAYER(KeyValues.PLAYER, KeyValues.ANONYMOUS, KeyValues.HOST),
  ANONYMOUS(KeyValues.ANONYMOUS, KeyValues.HOST),
  HOST(KeyValues.HOST, KeyValues.ADMIN, KeyValues.ANONYMOUS);

  /** Returns a set of keys that should be allowed access given a target role. */
  @Getter private ApiKey allowedKey;

  @Getter private Collection<ApiKey> disallowedKeys;

  AllowedUserRole(final String allowedKey, final String... disallowedKeys) {
    this.allowedKey = ApiKey.of(allowedKey);
    this.disallowedKeys =
        Arrays.stream(disallowedKeys).map(ApiKey::of).collect(Collectors.toList());
  }

  /** These api key values were SHA512 hashed and are stored in 'integration.yml' */
  private static class KeyValues {
    static final String ADMIN = "ADMIN";
    static final String MODERATOR = "MODERATOR";
    static final String PLAYER = "PLAYER";
    static final String ANONYMOUS = "ANONYMOUS";
    static final String HOST = "HOST";
  }
}
