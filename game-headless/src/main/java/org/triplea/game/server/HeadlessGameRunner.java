package org.triplea.game.server;

import games.strategy.engine.framework.startup.ui.PlayerTypes;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.triplea.ai.does.nothing.DoesNothingAiProvider;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.injection.Injections;

/** Runs a headless game server. */
public final class HeadlessGameRunner {
  private HeadlessGameRunner() {}

  /**
   * Entry point for running a new headless game server. The headless game server runs until the
   * process is killed or the headless game server is shut down via administrative command.
   */
  public static void main(final String[] args) {
    Injections.init(constructInjections());
    HeadlessGameServer.start(args);
  }

  private static Injections constructInjections() {
    return Injections.builder()
        .engineVersion(new ProductVersionReader().getVersion())
        .playerTypes(gatherPlayerTypes())
        .build();
  }

  private static Collection<PlayerTypes.Type> gatherPlayerTypes() {
    return Stream.of(
            PlayerTypes.getBuiltInPlayerTypes(),
            List.of(new PlayerTypes.AiType(new DoesNothingAiProvider())))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }
}
