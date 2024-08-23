package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.framework.I18nEngineFramework;
import games.strategy.engine.player.Player;
import games.strategy.triplea.ai.fast.FastAi;
import games.strategy.triplea.ai.pro.ProAi;
import games.strategy.triplea.ai.weak.WeakAi;
import java.util.Collection;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;

@Value
public class PlayerTypes {

  public static final String DOES_NOTHING_PLAYER_LABEL = "Does Nothing (AI)";
  public static final Type WEAK_AI =
      new Type(I18nEngineFramework.get().getText("startup.PlayerTypes.PLAYER_TYPE_AI_EASY_LABEL")) {
        @Override
        public Player newPlayerWithName(final String name) {
          return new WeakAi(name, getLabel());
        }
      };
  public static final Type FAST_AI =
      new Type(I18nEngineFramework.get().getText("startup.PlayerTypes.PLAYER_TYPE_AI_FAST_LABEL")) {
        @Override
        public Player newPlayerWithName(final String name) {
          return new FastAi(name, getLabel());
        }
      };
  public static final Type PRO_AI =
      new Type(I18nEngineFramework.get().getText("startup.PlayerTypes.PLAYER_TYPE_AI_HARD_LABEL")) {
        @Override
        public Player newPlayerWithName(final String name) {
          return new ProAi(name, getLabel());
        }
      };
  public static final String PLAYER_TYPE_DEFAULT_LABEL =
      I18nEngineFramework.get().getText("startup.PlayerTypes.PLAYER_TYPE_DEFAULT_LABEL");
  public static final String PLAYER_TYPE_HUMAN_LABEL =
      I18nEngineFramework.get().getText("startup.PlayerTypes.PLAYER_TYPE_HUMAN_LABEL");

  Collection<Type> playerTypes;

  public PlayerTypes(final Collection<Type> playerTypes) {
    this.playerTypes = playerTypes;
  }

  public static Collection<Type> getBuiltInPlayerTypes() {
    return List.of(PlayerTypes.WEAK_AI, PlayerTypes.FAST_AI, PlayerTypes.PRO_AI);
  }

  /**
   * Returns the set of player types that users can select.
   *
   * @return Human readable player type labels.
   */
  public String[] getAvailablePlayerLabels() {
    return playerTypes.stream().filter(Type::isVisible).map(Type::getLabel).toArray(String[]::new);
  }

  /**
   * Converter function, each player type has a label, this method will convert from a given label
   * value to the corresponding enum.
   *
   * @throws IllegalStateException Thrown if the given label does not match any in the current enum.
   */
  public Type fromLabel(final String label) {
    return playerTypes.stream()
        .filter(playerType -> playerType.getLabel().equals(label))
        .findAny()
        .orElseThrow(() -> new IllegalStateException("could not find PlayerType: " + label));
  }

  /**
   * PlayerType indicates what kind of entity is controlling a player, whether an AI, human, or a
   * remote (network) player.
   */
  @AllArgsConstructor
  @EqualsAndHashCode
  public abstract static class Type {

    @Getter private final String label;

    @Getter(AccessLevel.PACKAGE)
    private final boolean visible;

    public Type(final String label) {
      this(label, true);
    }

    /**
     * Each PlayerType is backed by an {@code IRemotePlayer} instance. Given a player name this
     * method will create the corresponding {@code IRemotePlayer} instance.
     */
    public abstract Player newPlayerWithName(String name);

    @Override
    public String toString() {
      return label;
    }
  }
}
