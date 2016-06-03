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

public class PlayerOrder {
  private final List<PlayerID> m_playerSet = new ArrayList<>();

  private <E> Set<E> removeDups(final Collection<E> c) {
    return new LinkedHashSet<>(c);
  }

  protected void saveToFile(final PrintGenerationData printData) throws IOException {
    GameData m_data = printData.getData();
    PrintGenerationData m_printData = printData;
    Iterator<GameStep> m_gameStepIterator = m_data.getSequence().iterator();
    while (m_gameStepIterator.hasNext()) {
      final GameStep currentStep = m_gameStepIterator.next();
      if (currentStep.getDelegate() != null && currentStep.getDelegate().getClass() != null) {
        final String delegateClassName = currentStep.getDelegate().getClass().getName();
        if (delegateClassName.equals("games.strategy.triplea.delegate.InitializationDelegate")
            || delegateClassName.equals("games.strategy.triplea.delegate.BidPurchaseDelegate")
            || delegateClassName.equals("games.strategy.triplea.delegate.BidPlaceDelegate")
            || delegateClassName.equals("games.strategy.triplea.delegate.EndRoundDelegate")) {
          continue;
        }
      } else if (currentStep.getName() != null
          && (currentStep.getName().endsWith("Bid") || currentStep.getName().endsWith("BidPlace"))) {
        continue;
      }
      final PlayerID currentPlayerID = currentStep.getPlayerID();
      if (currentPlayerID != null && !currentPlayerID.isNull()) {
        m_playerSet.add(currentPlayerID);
      }
    }
    FileWriter turnWriter = null;
    m_printData.getOutDir().mkdir();
    final File outFile = new File(m_printData.getOutDir(), "General Information.csv");
    turnWriter = new FileWriter(outFile, true);
    turnWriter.write("Turn Order\r\n");
    final Set<PlayerID> noDuplicates = removeDups(m_playerSet);
    final Iterator<PlayerID> playerIterator = noDuplicates.iterator();
    int count = 1;
    while (playerIterator.hasNext()) {
      final PlayerID currentPlayerID = playerIterator.next();
      turnWriter.write(count + ". " + currentPlayerID.getName() + "\r\n");
      count++;
    }
    turnWriter.close();
  }
}
