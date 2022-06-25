package games.strategy.engine.framework.startup.mc;

import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.engine.player.Player;
import games.strategy.triplea.TripleAPlayer;
import games.strategy.triplea.settings.ClientSetting;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.triplea.ai.does.nothing.DoesNothingAi;
import org.triplea.ai.flowfield.FlowFieldAi;

public class HeadedPlayerTypes {

  private static final String FLOW_FIELD_LABEL = "FlowField (AI)";

  public static final PlayerTypes.Type HUMAN_PLAYER =
      new PlayerTypes.Type("Human") {
        @Override
        public Player newPlayerWithName(final String name) {
          return new TripleAPlayer(name, getLabel(), false);
        }
      };
  /** A hidden player type to represent network connected players. */
  public static final PlayerTypes.Type CLIENT_PLAYER =
      new PlayerTypes.Type("Client", false) {
        @Override
        public Player newPlayerWithName(final String name) {
          return new TripleAPlayer(name, getLabel(), true);
        }
      };

  private static boolean filterBetaPlayerType(final PlayerTypes.Type playerType) {
    if (playerType.getLabel().equals(FLOW_FIELD_LABEL)) {
      return ClientSetting.showBetaFeatures.getValue().orElse(false);
    }
    return true;
  }

  private static PlayerTypes.Type getDoesNothingType() {
    return new PlayerTypes.Type(PlayerTypes.DOES_NOTHING_PLAYER_LABEL) {
      @Override
      public Player newPlayerWithName(String name) {
        return new DoesNothingAi(name, getLabel());
      }
    };
  }

  private static PlayerTypes.Type getFlowFieldType() {
    return new PlayerTypes.Type(FLOW_FIELD_LABEL) {
      @Override
      public Player newPlayerWithName(String name) {
        return new FlowFieldAi(name, getLabel());
      }
    };
  }

  public static Collection<PlayerTypes.Type> getPlayerTypes() {
    return Stream.of(
            // The first item in this list will be the default when hosting
            List.of(HUMAN_PLAYER),
            PlayerTypes.getBuiltInPlayerTypes(),
            List.of(CLIENT_PLAYER, getDoesNothingType(), getFlowFieldType()))
        .flatMap(Collection::stream)
        .filter(HeadedPlayerTypes::filterBetaPlayerType)
        .collect(Collectors.toList());
  }
}
