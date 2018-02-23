package games.strategy.triplea.printgenerator;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerID;
import games.strategy.triplea.delegate.BidPlaceDelegate;
import games.strategy.triplea.delegate.BidPurchaseDelegate;
import games.strategy.triplea.delegate.EndRoundDelegate;
import games.strategy.triplea.delegate.InitializationDelegate;

class PlayerOrder {
  private final List<PlayerID> playerSet = new ArrayList<>();

  private static <E> Set<E> removeDups(final Collection<E> c) {
    return new LinkedHashSet<>(c);
  }

  void saveToFile(final PrintGenerationData printData) throws IOException {
    final GameData gameData = printData.getData();
    for (final GameStep currentStep : gameData.getSequence()) {
      if ((currentStep.getDelegate() != null) && (currentStep.getDelegate().getClass() != null)) {
        final String delegateClassName = currentStep.getDelegate().getClass().getName();
        if (delegateClassName.equals(InitializationDelegate.class.getName())
            || delegateClassName.equals(BidPurchaseDelegate.class.getName())
            || delegateClassName.equals(BidPlaceDelegate.class.getName())
            || delegateClassName.equals(EndRoundDelegate.class.getName())) {
          continue;
        }
      } else if ((currentStep.getName() != null)
          && (currentStep.getName().endsWith("Bid") || currentStep.getName().endsWith("BidPlace"))) {
        continue;
      }
      final PlayerID currentPlayerId = currentStep.getPlayerId();
      if ((currentPlayerId != null) && !currentPlayerId.isNull()) {
        playerSet.add(currentPlayerId);
      }
    }
    printData.getOutDir().mkdir();
    final File outFile = new File(printData.getOutDir(), "General Information.csv");
    try (Writer turnWriter = Files.newBufferedWriter(
        outFile.toPath(),
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
      turnWriter.write("Turn Order\r\n");
      int count = 1;
      for (final PlayerID currentPlayerId : removeDups(playerSet)) {
        turnWriter.write(count + ". " + currentPlayerId.getName() + "\r\n");
        count++;
      }
    }
  }
}
