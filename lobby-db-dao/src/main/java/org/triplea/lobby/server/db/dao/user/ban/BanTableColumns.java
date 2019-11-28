package org.triplea.lobby.server.db.dao.user.ban;

import lombok.experimental.UtilityClass;

@UtilityClass
class BanTableColumns {
  public static final String PUBLIC_ID_COLUMN = "public_id";
  public static final String USERNAME_COLUMN = "username";
  public static final String SYSTEM_ID_COLUMN = "system_id";
  public static final String IP_COLUMN = "ip";
  public static final String BAN_EXPIRY_COLUMN = "ban_expiry";
  public static final String DATE_CREATED_COLUMN = "date_created";
}
