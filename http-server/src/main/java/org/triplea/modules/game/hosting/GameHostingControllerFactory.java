package org.triplea.modules.game.hosting;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.api.key.ApiKeyDaoWrapper;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GameHostingControllerFactory {

  public static GameHostingController buildController(final Jdbi jdbi) {
    final ApiKeyDaoWrapper apiKeyDaoWrapper = new ApiKeyDaoWrapper(jdbi);
    return GameHostingController.builder().apiKeySupplier(apiKeyDaoWrapper::newGameHostKey).build();
  }
}
