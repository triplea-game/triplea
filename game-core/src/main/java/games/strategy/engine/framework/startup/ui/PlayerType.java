package games.strategy.engine.framework.startup.ui;

import java.util.Arrays;
import java.util.function.Function;

import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.triplea.TripleAPlayer;
import games.strategy.triplea.ai.fast.FastAi;
import games.strategy.triplea.ai.pro.ProAi;
import games.strategy.triplea.ai.weak.DoesNothingAi;
import games.strategy.triplea.ai.weak.WeakAi;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * PlayerType indicates what kind of entity is controlling a player, whether an AI, human,
 * or a remote (network) player.
 */
@AllArgsConstructor
public enum PlayerType {
  HUMAN_PLAYER("Human", nationName -> new TripleAPlayer(nationName) {
    @Override
    public PlayerType getPlayerType() {
      return PlayerType.HUMAN_PLAYER;
    }
  }),

  WEAK_AI("Easy (AI)", WeakAi::new),

  FAST_AI("Fast (AI)", FastAi::new),

  PRO_AI("Hard (AI)", ProAi::new),

  DOES_NOTHING_AI("Does Nothing (AI)", DoesNothingAi::new),

  /**
   * A hidden player type to represent network connected players.
   */
  CLIENT_PLAYER("Client", false, nationName -> new TripleAPlayer(nationName) {
    @Override
    public PlayerType getPlayerType() {
      return PlayerType.CLIENT_PLAYER;
    }
  }),

  /**
   * A 'dummy' player type used for battle calc.
   */
  BATTLE_CALC_DUMMY("None (AI)", false, name -> {
    throw new UnsupportedOperationException(
        "Fail fast - bad configuration, should instantiate dummy player type only for battle calc");
  });

  @Getter
  private final String label;
  @Getter(AccessLevel.PRIVATE)
  private final boolean visible;
  private final Function<String, IGamePlayer> playerFactory;

  PlayerType(String label, Function<String, IGamePlayer> playerFactory) {
    this(label, true, playerFactory);
  }

  /**
   * Returns the set of player types that users can select.
   * 
   * @return Human readable player type labels.
   */
  public static String[] playerTypes() {
    return Arrays.stream(values())
        .filter(PlayerType::isVisible)
        .map(enumValue -> enumValue.label)
        .toArray(String[]::new);
  }

  /**
   * Each PlayerType is backed by an @{code IGamePlayer} instance. Given a playername this method
   * will create the corresponding @{code IGamePlayer} instance.
   */
  public IGamePlayer createPlayerWithName(String name) {
    return playerFactory.apply(name);
  }

  /**
   * Converter function, each player type has a label, this method will convert from a given label
   * value to the corresponding enum.
   *
   * @throws IllegalStateException Thrown if the given label does not match any in the current enum.
   */
  public static PlayerType fromLabel(final String label) {
    return Arrays.stream(values())
        .filter(enumValue -> enumValue.label.equals(label))
        .findAny()
        .orElseThrow(() -> new IllegalStateException("could not find PlayerType: " + label));
  }
}
