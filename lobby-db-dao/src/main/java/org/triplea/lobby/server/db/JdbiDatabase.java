package org.triplea.lobby.server.db;

import java.util.List;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import com.google.common.collect.ImmutableList;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility to get connections to the Postgres lobby database.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JdbiDatabase {
  public static final List<Class> BEAN_MAPPER_CLASSES =
      ImmutableList.of(ModeratorAuditHistoryItem.class);

  /**
   * Creates a new connection to database. This connection should
   * only be used by the TripleA Java Lobby. DropWizard will create
   * a connection from configuration automatically.
   */
  public static Jdbi newConnection() {
    final Jdbi jdbi = Jdbi.create(
        String.format(
            "jdbc:postgresql://%s:%s/%s",
            DatabaseEnvironmentVariable.POSTGRES_HOST.getValue(),
            DatabaseEnvironmentVariable.POSTGRES_PORT.getValue(),
            DatabaseEnvironmentVariable.POSTGRES_DATABASE.getValue()),
        DatabaseEnvironmentVariable.POSTGRES_USER.getValue(),
        DatabaseEnvironmentVariable.POSTGRES_PASSWORD.getValue());
    jdbi.installPlugin(new SqlObjectPlugin());

    BEAN_MAPPER_CLASSES.forEach(mapperClass -> jdbi.registerRowMapper(BeanMapper.factory(mapperClass)));
    return jdbi;
  }
}
