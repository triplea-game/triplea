package org.triplea.http.client;

import lombok.experimental.UtilityClass;

/**
 * Shared data class for all server paths, both server and client will use these variables for URI
 * paths.
 */
@UtilityClass
public class ServerPaths {
  public static final String ERROR_REPORT_PATH = "/support/error-report";
  public static final String CAN_UPLOAD_ERROR_REPORT_PATH = "/support/error-report-check";
  public static final String GET_MAP_TAGS_META_DATA_PATH = "/support/maps/list-tags";
  public static final String UPDATE_MAP_TAG_PATH = "/support/maps/update-tag";
  public static final String MAPS_LISTING_PATH = "/support/maps/listing";

  public static final String FORGOT_PASSWORD_PATH = "/lobby/forgot-password";
  public static final String LATEST_VERSION_PATH = "/support/latest-version";
  public static final String GAME_HOSTING_REQUEST_PATH = "/lobby/game-hosting-request";

  public static final String IS_PLAYER_BANNED_PATH = "/lobby/remote/actions/is-player-banned";
  public static final String SEND_SHUTDOWN_PATH = "/lobby/remote/actions/send-shutdown";

  public static final String FETCH_GAMES_PATH = "/lobby/games/fetch-games";
  public static final String BOOT_GAME_PATH = "/lobby/games/boot-game";

  public static final String KEEP_ALIVE_PATH = "/lobby/games/keep-alive";
  public static final String POST_GAME_PATH = "/lobby/games/post-game";
  public static final String UPDATE_GAME_PATH = "/lobby/games/update-game";
  public static final String REMOVE_GAME_PATH = "/lobby/games/remove-game";
  public static final String PLAYER_JOINED_PATH = "/lobby/games/player-joined";
  public static final String PLAYER_LEFT_PATH = "/lobby/games/player-left";

  public static final String LOGIN_PATH = "/lobby/user-login/authenticate";
  public static final String CREATE_ACCOUNT = "/lobby/user-login/create-account";

  public static final String REMOVE_BANNED_USER_NAME_PATH =
      "/lobby/moderator-toolbox/remove-username-ban";
  public static final String ADD_BANNED_USER_NAME_PATH =
      "/lobby/moderator-toolbox/add-username-ban";
  public static final String GET_BANNED_USER_NAMES_PATH =
      "/lobby/moderator-toolbox/get-username-bans";

  public static final String GET_USER_BANS_PATH = "/lobby/moderator-toolbox/get-user-bans";
  public static final String REMOVE_USER_BAN_PATH = "/lobby/moderator-toolbox/remove-user-ban";
  public static final String BAN_USER_PATH = "/lobby/moderator-toolbox/ban-user";

  public static final String FETCH_ACCESS_LOG_PATH = "/lobby/moderator-toolbox/access-log";
  public static final String AUDIT_HISTORY_PATH = "/lobby/moderator-toolbox/audit-history/lookup";

  public static final String FETCH_MODERATORS_PATH = "/lobby/moderator-toolbox/fetch-moderators";
  public static final String IS_ADMIN_PATH = "/lobby/moderator-toolbox/is-admin";
  public static final String CHECK_USER_EXISTS_PATH = "/lobby/moderator-toolbox/does-user-exist";
  public static final String REMOVE_MOD_PATH = "/lobby/moderator-toolbox/admin/remove-mod";
  public static final String ADD_ADMIN_PATH = "/lobby/moderator-toolbox/admin/add-super-mod";
  public static final String ADD_MODERATOR_PATH = "/lobby/moderator-toolbox/admin/add-moderator";

  public static final String BAD_WORD_ADD_PATH = "/lobby/moderator-toolbox/bad-words/add";
  public static final String BAD_WORD_REMOVE_PATH = "/lobby/moderator-toolbox/bad-words/remove";
  public static final String BAD_WORD_GET_PATH = "/lobby/moderator-toolbox/bad-words/get";

  public static final String DISCONNECT_PLAYER_PATH = "/lobby/moderator/disconnect-player";
  public static final String BAN_PLAYER_PATH = "/lobby/moderator/ban-player";
  public static final String MUTE_USER = "/lobby/moderator/mute-player";

  public static final String FETCH_PLAYER_INFORMATION = "/lobby/fetch-player-info";
  public static final String FETCH_PLAYERS_IN_GAME = "/lobby/fetch-players-in-game";

  public static final String CHANGE_PASSWORD_PATH = "/lobby/user-account/change-password";
  public static final String FETCH_EMAIL_PATH = "/lobby/user-account/fetch-email";
  public static final String CHANGE_EMAIL_PATH = "/lobby/user-account/change-email";
}
