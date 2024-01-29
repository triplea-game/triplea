package org.triplea.server.error.reporting.upload;

import java.time.Instant;
import java.util.Optional;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/** DAO class for error reporting functionality. */
public interface ErrorReportingDao {

  /** Inserts a new record indicating a user has submitted an error report at a given date. */
  @SqlUpdate(
      "insert into error_report_history"
          + "(user_ip, system_id, report_title, game_version, created_issue_link) "
          + "values"
          + "(:ip, :systemId, :title, :gameVersion, :githubIssueLink)")
  void insertHistoryRecord(@BindBean InsertHistoryRecordParams insertHistoryRecordParams);

  /**
   * Method to clean up old records from the error report history table. This is to avoid the table
   * from growing very large.
   *
   * @param purgeSinceDate Any records older than this date will be removed.
   */
  @SqlUpdate("delete from error_report_history where date_created < :purgeSinceDate")
  void purgeOld(@Bind("purgeSinceDate") Instant purgeSinceDate);

  @SqlQuery(
      "select created_issue_link"
          + "  from error_report_history"
          + "  where report_title = :reportTitle "
          + "    and game_version = :gameVersion")
  Optional<String> getErrorReportLink(
      @Bind("reportTitle") String reportTitle, @Bind("gameVersion") String gameVersion);
}
