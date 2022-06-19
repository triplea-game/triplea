package games.strategy.engine.framework.startup.mc;

import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.engine.player.Player;
import games.strategy.triplea.TripleAPlayer;
import games.strategy.triplea.settings.ClientSetting;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.triplea.ai.does.nothing.DoesNothingAiProvider;
import org.triplea.ai.flowfield.FlowFieldAiProvider;

public class HeadedPlayerTypes {

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
    if (playerType.getLabel().equals("FlowField (AI)")) {
      return ClientSetting.showBetaFeatures.getValue().orElse(false);
    }
    return true;
  }

  public static Collection<PlayerTypes.Type> getPlayerTypes() {
    return Stream.of(
            PlayerTypes.getBuiltInPlayerTypes(),
            List.of(
                HUMAN_PLAYER,
                CLIENT_PLAYER,
                new PlayerTypes.AiType(new DoesNothingAiProvider()),
                new PlayerTypes.AiType(new FlowFieldAiProvider())))
        .flatMap(Collection::stream)
        .filter(HeadedPlayerTypes::filterBetaPlayerType)
        .collect(Collectors.toList());
  }
}
