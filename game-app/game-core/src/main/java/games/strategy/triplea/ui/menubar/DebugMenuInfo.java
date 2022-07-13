package games.strategy.triplea.ui.menubar;

import games.strategy.engine.player.Player;
import games.strategy.triplea.ui.menubar.debug.AiPlayerDebugOption;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.Value;

/** Class to separate UI concerns from static data. */
public class DebugMenuInfo {

  @Value
  private static class DebugOption {
    String name;
    List<AiPlayerDebugOption> options;
  }

  private static final List<Consumer<Frame>> frameVisitors = new ArrayList<>();

  private static final List<DebugOption> debugOptions = new ArrayList<>();

  public static void registerDebugOptions(
      final Player player, final List<AiPlayerDebugOption> options) {
    registerDebugOptions(player.getName(), options);
  }

  public static void registerDebugOptions(
      final String playerName, final List<AiPlayerDebugOption> options) {
    debugOptions.add(new DebugOption(playerName, options));
  }

  public static void registerFrameVisitor(final Consumer<Frame> visitor) {
    frameVisitors.add(visitor);
  }

  public static boolean isEmpty() {
    return frameVisitors.isEmpty() && debugOptions.isEmpty();
  }

  public static void visit(Frame frame) {
    frameVisitors.forEach(frameConsumer -> frameConsumer.accept(frame));
  }

  public static void runForOption(BiConsumer<String, List<AiPlayerDebugOption>> biConsumer) {
    debugOptions.stream()
        .sorted(Comparator.comparing(DebugOption::getName))
        .forEach(debugOption -> biConsumer.accept(debugOption.getName(), debugOption.getOptions()));
  }
}
