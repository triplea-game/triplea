package org.triplea.db.dao.api.key;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import java.util.function.Function;
import org.triplea.domain.data.ApiKey;

@SuppressWarnings("UnstableApiUsage")
public class ApiKeyHasher implements Function<ApiKey, String> {
  @Override
  public String apply(final ApiKey apiKey) {
    return Hashing.sha512().hashString(apiKey.getValue(), Charsets.UTF_8).toString();
  }
}
