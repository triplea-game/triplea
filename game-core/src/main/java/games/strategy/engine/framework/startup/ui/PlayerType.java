package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.player.Player;
import games.strategy.triplea.TripleAPlayer;
import games.strategy.triplea.ai.fast.FastAi;
import games.strategy.triplea.ai.pro.ProAi;
import games.strategy.triplea.ai.weak.DoesNothingAi;
import games.strategy.triplea.ai.weak.WeakAi;
import java.util.Arrays;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * PlayerType indicates what kind of entity is controlling a player, whether an AI, human, or a
 * remote (network) player.
 */
@AllArgsConstructor
public enum PlayerType {
  HUMAN_PLAYER("Human") {
    @Override
    public Player newPlayerWithName(final String name) {
      return new TripleAPlayer(name) {
        @Override
        public PlayerType getPlayerType() {
          return HUMAN_PLAYER;
        }
      };
    }
  },

  WEAK_AI("Easy (AI)") {
    @Override
    public Player newPlayerWithName(final String name) {
      return new WeakAi(name);
    }
  },

  FAST_AI("Fast (AI)") {
    @Override
    public Player newPlayerWithName(final String name) {
      return new FastAi(name);
    }
  },

  PRO_AI("Hard (AI)") {
    @Override
    public Player newPlayerWithName(final String name) {
      return new ProAi(name);
    }
  },

  DOES_NOTHING_AI("Does Nothing (AI)") {
    @Override
    public Player newPlayerWithName(final String name) {
      return new DoesNothingAi(name);
    }
  },

  /** A hidden player type to represent network connected players. */
  CLIENT_PLAYER("Client", false) {
    @Override
    public Player newPlayerWithName(final String name) {
      return new TripleAPlayer(name) {
        @Override
        public PlayerType getPlayerType() {
          return CLIENT_PLAYER;
        }
      };
    }
  },

  /** A 'dummy' player type used for battle calc. */
  BATTLE_CALC_DUMMY("None (AI)", false) {
    @Override
    public Player newPlayerWithName(final String name) {
      throw new UnsupportedOperationException(
          "Fail fast - bad configuration, should instantiate dummy player "
              + "type only for battle calc");
    }
  };

  @Getter private final String label;

  @Getter(AccessLevel.PRIVATE)
  private final boolean visible;

  PlayerType(final String label) {
    this(label, true);
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
   * Each PlayerType is backed by an {@code IRemotePlayer} instance. Given a player name this method
   * will create the corresponding {@code IRemotePlayer} instance.
   */
  public abstract Player newPlayerWithName(String name);

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
