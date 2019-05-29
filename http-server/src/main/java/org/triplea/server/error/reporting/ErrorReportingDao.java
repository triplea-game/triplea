package org.triplea.server.error.reporting;

import java.time.Instant;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * DAO class for error reporting functionality.
 */
public interface ErrorReportingDao {

  /**
   * Inserts a new record indicating a user has submitted an error report
   * at a given date.
   *
   * @param userIp Ip host address of the user submitting an error report.
   */
  @SqlUpdate("insert into error_report_history(user_ip) values(:ip)")
  void insertHistoryRecord(@Bind("ip") String userIp);


  /**
   * Counts how many records, how many error reports, a user has submitted
   * since a given date.
   *
   * @param userIp Ip host address of the user submitting an error report.
   * @param dateSince How far back to look, only records newer compared
   *        to this date will be counted.
   */
  @SqlQuery("select count(*) from error_report_history "
      + "where user_ip = :ip "
      + "  and date_created > :dateSince")
  int countRecordsByIpSince(
      @Bind("ip") String userIp, @Bind("dateSince") Instant dateSince);

  /**
   * Method to clean up old records from the error report history table.
   * This is to avoid the table from growing very large.
   *
   * @param purgeSinceDate Any records older than this date will be removed.
   */
  @SqlUpdate("delete from error_report_history where date_created < :purgeSinceDate")
  void purgeOld(@Bind("purgeSinceDate") Instant purgeSinceDate);
}
