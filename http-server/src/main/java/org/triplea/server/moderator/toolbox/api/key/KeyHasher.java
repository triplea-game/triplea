package org.triplea.server.moderator.toolbox.api.key;

import com.google.common.base.Preconditions;
import java.util.function.BiFunction;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.triplea.server.http.AppConfig;

/** Utility class to provide hashing functions for moderator API key. */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public final class KeyHasher {

  @Nonnull private final String keySalt;
  @Nonnull private final BiFunction<String, String, String> hashFunction;

  public KeyHasher(final AppConfig appConfig) {
    this(appConfig.getBcryptSalt(), BCrypt::hashpw);
  }

  public String applyHash(final String apiKey, final String password) {
    Preconditions.checkNotNull(apiKey);
    Preconditions.checkNotNull(password);
    return applyHash(apiKey + password);
  }

  public String applyHash(final String valueToHash) {
    Preconditions.checkNotNull(valueToHash);
    Preconditions.checkArgument(!valueToHash.isEmpty());

    return hashFunction.apply(valueToHash, keySalt);
  }
}
