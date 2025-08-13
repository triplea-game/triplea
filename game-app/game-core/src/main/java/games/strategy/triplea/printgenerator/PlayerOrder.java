package games.strategy.triplea.printgenerator;

import static games.strategy.triplea.printgenerator.UnitInformation.FILE_NAME_GENERAL_INFORMATION_CSV;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameStep;
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
import java.util.Set;

class PlayerOrder {
  private final List<GamePlayer> playerSet = new ArrayList<>();

  private static <E> Set<E> removeDupes(final Collection<E> c) {
    return new LinkedHashSet<>(c);
  }

  void saveToFile(final PrintGenerationData printData) throws IOException {
    for (final GameStep currentStep : printData.getData().getSequence()) {
      if (currentStep.getDelegate() != null) {
        final String delegateClassName = currentStep.getDelegate().getClass().getName();
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
    final Path outFile = printData.getOutDir().resolve(FILE_NAME_GENERAL_INFORMATION_CSV);
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
