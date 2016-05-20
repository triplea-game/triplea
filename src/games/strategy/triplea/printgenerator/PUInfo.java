package games.strategy.triplea.printgenerator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;

public class PUInfo {
  private GameData m_data;
  private final Map<PlayerID, Map<Resource, Integer>> m_infoMap = new HashMap<>();
  private Iterator<PlayerID> m_playerIterator;
  private PrintGenerationData m_printData;

  protected void saveToFile(final PrintGenerationData printData) {
    m_data = printData.getData();
    m_printData = printData;
    m_playerIterator = m_data.getPlayerList().iterator();
    while (m_playerIterator.hasNext()) {
      final PlayerID currentPlayer = m_playerIterator.next();
      final Iterator<Resource> resourceIterator = m_data.getResourceList().getResources().iterator();
      final Map<Resource, Integer> resourceMap = new HashMap<>();
      while (resourceIterator.hasNext()) {
        final Resource currentResource = resourceIterator.next();
        final Integer amountOfResource = currentPlayer.getResources().getQuantity(currentResource);
        resourceMap.put(currentResource, amountOfResource);
      }
      m_infoMap.put(currentPlayer, resourceMap);
    }
    FileWriter resourceWriter = null;
    try {
      final File outFile = new File(m_printData.getOutDir(), "General Information.csv");
      resourceWriter = new FileWriter(outFile, true);
      // Print Title
      final int numResources = m_data.getResourceList().size();
      for (int i = 0; i < numResources / 2 - 1 + numResources % 2; i++) {
        resourceWriter.write(",");
      }
      resourceWriter.write("Resource Chart");
      for (int i = 0; i < numResources / 2 - numResources % 2; i++) {
        resourceWriter.write(",");
      }
      resourceWriter.write("\r\n");
      // Print Resources
      final Iterator<Resource> resourceIterator = m_data.getResourceList().getResources().iterator();
      resourceWriter.write(",");
      while (resourceIterator.hasNext()) {
        final Resource currentResource = resourceIterator.next();
        resourceWriter.write(currentResource.getName() + ",");
      }
      resourceWriter.write("\r\n");
      // Print Player's and Resource Amount's
      m_playerIterator = m_data.getPlayerList().iterator();
      while (m_playerIterator.hasNext()) {
        final PlayerID currentPlayer = m_playerIterator.next();
        resourceWriter.write(currentPlayer.getName());
        final Map<Resource, Integer> resourceMap = m_infoMap.get(currentPlayer);
        final Iterator<Resource> resIterator = resourceMap.keySet().iterator();
        while (resIterator.hasNext()) {
          final Resource currentResource = resIterator.next();
          final Integer amountResource = resourceMap.get(currentResource);
          resourceWriter.write("," + amountResource);
        }
        resourceWriter.write("\r\n");
      }
      resourceWriter.write("\r\n");
      resourceWriter.close();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }
}
