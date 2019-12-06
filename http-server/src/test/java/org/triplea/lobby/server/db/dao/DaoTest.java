package org.triplea.lobby.server.db.dao;

import com.github.database.rider.junit5.DBUnitExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.lobby.server.db.JdbiDatabase;
import org.triplea.test.common.Integration;

@Integration
@ExtendWith(DBUnitExtension.class)
@SuppressWarnings("PrivateConstructorForUtilityClass")
public abstract class DaoTest {

  protected static <T> T newDao(final Class<T> classType) {
    return JdbiDatabase.newConnection().onDemand(classType);
  }
}
