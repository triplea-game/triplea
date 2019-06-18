package org.triplea.lobby.server.db.dao;

import javax.annotation.Nonnull;

import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.triplea.lobby.server.db.PublicIdSupplier;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * DAO to manage the transaction of invalidating a single use key and inserting a new key.
 */
public interface ModeratorKeyRegistrationDao {

  /**
   * This method is a hack so that the interface has a JDBI annotation on it.
   * This lets us have a default method with a '@Transaction' on it.
   */
  @SuppressWarnings("unused")
  @VisibleForTesting
  @SqlQuery("select 1")
  int dummyQuery();

  /**
   * Parameter objects when setting a single-use key as used and registering a newly generated api-key.
   */
  @Builder
  @ToString
  @EqualsAndHashCode
  class Params {
    @Nonnull
    private final Integer userId;
    @Nonnull
    private final String singleUseKey;
    @Nonnull
    private final String newKey;
    @Nonnull
    private final ModeratorSingleUseKeyDao singleUseKeyDao;
    @Nonnull
    private final ModeratorApiKeyDao apiKeyDao;
    @Nonnull
    private final String registeringMachineIp;

    private void checkState() {
      Preconditions.checkState(userId > 0);
      Preconditions.checkState(singleUseKey.length() > 16);
      Preconditions.checkState(newKey.length() > 16);
      Preconditions.checkState(!singleUseKey.equalsIgnoreCase(newKey));
    }
  }

  @Transaction
  default void invalidateSingleUseKeyAndGenerateNew(Params params) {
    params.checkState();
    final String publicKeyId = new PublicIdSupplier().get();
    Preconditions.checkState(params.singleUseKeyDao.invalidateSingleUseKey(params.singleUseKey) == 1);
    Preconditions.checkState(params.apiKeyDao.insertNewApiKey(
        publicKeyId, params.userId, params.registeringMachineIp, params.newKey) == 1);
  }
}
