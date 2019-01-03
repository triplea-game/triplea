package games.strategy.engine.framework.headless.game.server;

import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;

import games.strategy.engine.framework.ArgParsingHelper;
import games.strategy.triplea.UrlConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enum that represents each CLI option that can be provided to a headless server, and for each one
 * relevant configuration options.
 */
@AllArgsConstructor
public enum HeadlessGameServerCliParam {
  TRIPLEA_NAME("triplea.name"),

  TRIPLEA_PORT("triplea.port"),

  LOBBY_HOST("triplea.lobby.host"),

  LOBBY_PORT("triplea.lobby.port"),

  MAP_FOLDER("triplea.map.folder"),

  LOBBY_GAME_SUPPORT_EMAIL("triplea.lobby.game.supportEmail", Required.NOT_REQUIRED),

  LOBBY_GAME_SUPPORT_PASSWORD("triplea.lobby.game.supportPassword", Required.NOT_REQUIRED),

  LOBBY_GAME_COMMENTS("triplea.lobby.game.comments", Required.NOT_REQUIRED),

  LOBBY_GAME_RECONNECTION("triplea.lobby.game.reconnection", Required.NOT_REQUIRED),

  SERVER_PASSWORD("triplea.server.password", Required.NOT_REQUIRED);

  @Getter private final String label;
  private final Required required;

  HeadlessGameServerCliParam(final String label) {
    this(label, Required.REQUIRED);
  }

  @Override
  public String toString() {
    return label;
  }

  /**
   * Verify that all required args are present.
   *
   * @param args Expecting the raw String array provided to #main method.
   */
  public static ArgValidationResult validateArgs(final String... args) {
    final Properties properties = ArgParsingHelper.getTripleaProperties(args);

    return new ArgValidationResult(
        Arrays.stream(values())
            .filter(param -> param.required == Required.REQUIRED)
            .filter(param -> properties.getProperty(param.label) == null)
            .map(
                param ->
                    "Did not find param: -"
                        + ArgParsingHelper.TRIPLEA_PROPERTY_PREFIX
                        + param.label)
            .collect(Collectors.toList()));
  }

  public static String exampleUsage() {
    return example(TRIPLEA_NAME, "Auto-Von-Bot(host_name__no_spaces_allowed)")
        + example(TRIPLEA_PORT, "4000(replace_with_your_bot_port_number)")
        + example(LOBBY_HOST, "lobby.triplea-game.org")
        + example(LOBBY_PORT, "3304")
        + example(MAP_FOLDER, "/home/triplea/maps")
        + "\nFor latest lobby host/port, check: "
        + UrlConstants.LOBBY_PROPS;
  }

  private static String example(final HeadlessGameServerCliParam param, final String value) {
    return "-" + ArgParsingHelper.TRIPLEA_PROPERTY_PREFIX + param.label + "=" + value + " ";
  }

  private enum Required {
    REQUIRED,

    NOT_REQUIRED;
  }
}
