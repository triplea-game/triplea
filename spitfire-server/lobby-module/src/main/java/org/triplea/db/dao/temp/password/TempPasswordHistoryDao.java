package org.triplea.db.dao.temp.password;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * DAO for CRUD operations on temp password history table. A table that stores a history of requests
 * for temporary passwords.
 */
public interface TempPasswordHistoryDao {

  /**
   * Returns the number of temp password requests made in the last day from a particular IP address.
   */
  @SqlQuery(
      "select count(*)"
          + " from temp_password_request_history"
          + " where inetaddress = :inetAddress::inet"
          + "   and date_created >  (now() - '1 day'::interval)")
  int countRequestsFromAddress(@Bind("inetAddress") String address);

  /**
   * Records a temp password request being made from a given IP address and for a given username.
   */
  @SqlUpdate(
      "insert into temp_password_request_history(inetaddress,  username)"
          + " values(:inetaddress::inet, :username)")
  void recordTempPasswordRequest(
      @Bind("inetaddress") String address, @Bind("username") String username);
}
