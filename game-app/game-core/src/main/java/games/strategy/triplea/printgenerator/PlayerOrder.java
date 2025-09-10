package games.strategy.triplea.printgenerator;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.triplea.delegate.BidPlaceDelegate;
import games.strategy.triplea.delegate.BidPurchaseDelegate;
import games.strategy.triplea.delegate.EndRoundDelegate;
import games.strategy.triplea.delegate.InitializationDelegate;
import games.strategy.triplea.util.PlayerOrderComparator;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class PlayerOrder {
  private final List<GamePlayer> playerSet = new ArrayList<>();

  private static <E> Set<E> removeDupes(final Collection<E> c) {
    return new LinkedHashSet<>(c);
  }

  void saveToFile(final PrintGenerationData printData) throws IOException {
    for (final GameStep currentStep : printData.getData().getSequence()) {
      Optional<IDelegate> optionalDelegate = currentStep.getDelegateOptional();
      if (optionalDelegate.isPresent()) {
        final String delegateClassName = optionalDelegate.get().getClass().getName();
        if (delegateClassName.equals(InitializationDelegate.class.getName())
            || delegateClassName.equals(BidPurchaseDelegate.class.getName())
            || delegateClassName.equals(BidPlaceDelegate.class.getName())
            || delegateClassName.equals(EndRoundDelegate.class.getName())) {
          continue;
        }
      } else if (currentStep.getName() != null
          && (currentStep.getName().endsWith("Bid")
              || currentStep.getName().endsWith("BidPlace"))) {
        continue;
      }
      final GamePlayer currentGamePlayer = currentStep.getPlayerId();
      if (currentGamePlayer != null && !currentGamePlayer.isNull()) {
        playerSet.add(currentGamePlayer);
      }
    }
    playerSet.sort(new PlayerOrderComparator(printData.getData()));
    Files.createDirectory(printData.getOutDir());
    final Path outFile = printData.getOutDir().resolve("General Information.csv");
    try (Writer turnWriter =
        Files.newBufferedWriter(
            outFile,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND)) {
      turnWriter.write("Turn Order\r\n");
      int count = 1;
      for (final GamePlayer currentGamePlayer : removeDupes(playerSet)) {
        turnWriter.write(count + ". " + currentGamePlayer.getName() + "\r\n");
        count++;
      }
    }
  }
}
