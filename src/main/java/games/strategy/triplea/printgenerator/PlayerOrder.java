package games.strategy.triplea.printgenerator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
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

public class PlayerOrder {
  private final List<PlayerID> playerSet = new ArrayList<>();

  private static <E> Set<E> removeDups(final Collection<E> c) {
    return new LinkedHashSet<>(c);
  }

  protected void saveToFile(final PrintGenerationData printData) throws IOException {
    final GameData gameData = printData.getData();
    final Iterator<GameStep> gameStepIterator = gameData.getSequence().iterator();
    while (gameStepIterator.hasNext()) {
      final GameStep currentStep = gameStepIterator.next();
      if (currentStep.getDelegate() != null && currentStep.getDelegate().getClass() != null) {
        final String delegateClassName = currentStep.getDelegate().getClass().getName();
        if (delegateClassName.equals(InitializationDelegate.class.getName())
            || delegateClassName.equals(BidPurchaseDelegate.class.getName())
            || delegateClassName.equals(BidPlaceDelegate.class.getName())
            || delegateClassName.equals(EndRoundDelegate.class.getName())) {
          continue;
        }
      } else if (currentStep.getName() != null
          && (currentStep.getName().endsWith("Bid") || currentStep.getName().endsWith("BidPlace"))) {
        continue;
      }
      final PlayerID currentPlayerId = currentStep.getPlayerID();
      if (currentPlayerId != null && !currentPlayerId.isNull()) {
        playerSet.add(currentPlayerId);
      }
    }
    FileWriter turnWriter = null;
    printData.getOutDir().mkdir();
    final File outFile = new File(printData.getOutDir(), "General Information.csv");
    turnWriter = new FileWriter(outFile, true);
    turnWriter.write("Turn Order\r\n");
    final Set<PlayerID> noDuplicates = removeDups(playerSet);
    final Iterator<PlayerID> playerIterator = noDuplicates.iterator();
    int count = 1;
    while (playerIterator.hasNext()) {
      final PlayerID currentPlayerId = playerIterator.next();
      turnWriter.write(count + ". " + currentPlayerId.getName() + "\r\n");
      count++;
    }
    turnWriter.close();
  }
}
