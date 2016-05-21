package games.strategy.triplea.printgenerator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.history.HistoryNode;
import games.strategy.triplea.attachments.UnitAttachment;

public class InitialSetup {
  private Iterator<UnitType> m_unitTypeIterator;
  private Iterator<PlayerID> m_playerIterator;
  private final Map<UnitType, UnitAttachment> m_unitInfoMap = new HashMap<>();
  private GameData m_data;
  private PrintGenerationData m_printData;

  protected InitialSetup() {}

  /**
   * @param GameData
   *        data
   * @param boolean useOriginalState
   */
  protected void run(final PrintGenerationData printData, final boolean useOriginalState) {
    m_data = printData.getData();
    m_printData = printData;
    if (useOriginalState) {
      final HistoryNode root = (HistoryNode) m_data.getHistory().getRoot();
      m_data.getHistory().gotoNode(root);
    }
    m_unitTypeIterator = m_data.getUnitTypeList().iterator();
    while (m_unitTypeIterator.hasNext()) {
      final UnitType currentType = m_unitTypeIterator.next();
      final UnitAttachment currentTypeUnitAttachment = UnitAttachment.get(currentType);
      m_unitInfoMap.put(currentType, currentTypeUnitAttachment);
    }
    new UnitInformation().saveToFile(m_printData, m_unitInfoMap);
    m_playerIterator = m_data.getPlayerList().iterator();
    while (m_playerIterator.hasNext()) {
      final PlayerID currentPlayer = m_playerIterator.next();
      new CountryChart().saveToFile(currentPlayer, m_printData);
    }
    new PUInfo().saveToFile(m_printData);
    try {
      new PlayerOrder().saveToFile(m_printData);
      new PUChart(m_printData).saveToFile();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }
}
