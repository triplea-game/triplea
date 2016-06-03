package games.strategy.triplea.printgenerator;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.triplea.Constants;
import games.strategy.util.IntegerMap;

class PUChart {
  private final Iterator<PlayerID> m_playerIterator;
  private final IntegerMap<PlayerID> m_moneyMap;
  private final int m_numPlayers;
  private final PlayerID[] m_playerArray;
  private final Integer[] m_moneyArray;
  private final Map<Integer, Integer> m_avoidMap;
  private final Font CHART_FONT = new Font("Serif", Font.PLAIN, 12);
  private final BufferedImage m_PUImage;
  private final Graphics2D m_g2d;
  private final File m_outDir;

  PUChart(final PrintGenerationData printData) {
    GameData m_data = printData.getData();
    m_playerIterator = m_data.getPlayerList().iterator();
    m_moneyMap = new IntegerMap<>();
    m_numPlayers = m_data.getPlayerList().size();
    m_playerArray = new PlayerID[m_numPlayers];
    m_moneyArray = new Integer[m_numPlayers];
    m_avoidMap = new HashMap<>();
    m_PUImage = new BufferedImage(600, 600, BufferedImage.TYPE_INT_ARGB);
    m_g2d = m_PUImage.createGraphics();
    m_outDir = printData.getOutDir();
  }

  private void initializeMap() {
    int count = 0;
    while (m_playerIterator.hasNext()) {
      final PlayerID currentPlayer = m_playerIterator.next();
      m_moneyMap.put(currentPlayer, currentPlayer.getResources().getQuantity(Constants.PUS));
      m_playerArray[count] = currentPlayer;
      m_moneyArray[count] = currentPlayer.getResources().getQuantity(Constants.PUS);
      count++;
    }
  }

  private void initializeAvoidMap() {
    for (int i = 0; i < m_numPlayers - 1; i++) {
      for (int j = i + 1; j < m_numPlayers; j++) {
        // i = firstPlayerMoney ; j = secondPlayerMoney
        if (m_moneyArray[i].equals(m_moneyArray[j])) {
          // s_avoidMap.put(s_playerArray[i], s_playerArray[j]);
          m_avoidMap.put(i, j);
        }
      }
    }
  }

  private void drawEllipseAndString(final int x, final int y, final String string) {
    m_g2d.setFont(CHART_FONT);
    m_g2d.draw(new Ellipse2D.Double(5 + 87 * x, 5 + 87 * y, 72, 72));
    final FontMetrics metrics = m_g2d.getFontMetrics();
    final int h = metrics.stringWidth(string) / 2;
    final int k = metrics.getHeight() / 2;
    m_g2d.drawString(string, 42 + 87 * x - h, 39 + 87 * y + k);
  }

  protected void saveToFile() throws IOException {
    initializeMap();
    initializeAvoidMap();
    int y_DIMENSION = 6;
    int x_DIMENSION = 7;
    final int numChartsNeeded = (int) Math.ceil(((double) m_moneyMap.totalValues()) / (x_DIMENSION * y_DIMENSION));
    for (int i = 0; i < numChartsNeeded; i++) {
      m_g2d.setColor(Color.black);
      // Draw Country Names
      for (int z = 0; z < m_playerArray.length; z++) {
        final int valMod42 = m_moneyArray[z] % 42;
        final int valModXDim = valMod42 % x_DIMENSION;
        final int valFloorXDim = valMod42 / x_DIMENSION;
        if (m_avoidMap.containsKey(z) && m_moneyArray[z] / 42 == i) {
          final FontMetrics metrics = m_g2d.getFontMetrics();
          final int width = metrics.stringWidth(m_playerArray[z].getName()) / 2;
          m_g2d.drawString(m_playerArray[z].getName(), 42 + 87 * valModXDim - width, 63 + 87 * valFloorXDim);
        } else if (m_avoidMap.containsValue(z) && m_moneyArray[z] / 42 == i) {
          final FontMetrics metrics = m_g2d.getFontMetrics();
          final int width = metrics.stringWidth(m_playerArray[z].getName()) / 2;
          m_g2d.drawString(m_playerArray[z].getName(), 42 + 87 * valModXDim - width, 30 + 87 * valFloorXDim);
        } else if (m_moneyArray[z] / 42 == i) {
          final FontMetrics metrics = m_g2d.getFontMetrics();
          final int width = metrics.stringWidth(m_playerArray[z].getName()) / 2;
          m_g2d.drawString(m_playerArray[z].getName(), 42 + 87 * valModXDim - width, 60 + 87 * valFloorXDim);
        }
      }
      // Draw Ellipses and Numbers
      for (int j = 0; j < y_DIMENSION; j++) {
        for (int k = 0; k < x_DIMENSION; k++) {
          final int numberincircle = x_DIMENSION * y_DIMENSION * i + x_DIMENSION * j + k;
          final String string = "" + numberincircle;
          drawEllipseAndString(k, j, string);
        }
      }
      // Write to file
      final int firstnum = x_DIMENSION * y_DIMENSION * i;
      final int secondnum = x_DIMENSION * y_DIMENSION * (i + 1) - 1;
      final File outputfile = new File(m_outDir, "PUchart" + firstnum + "-" + secondnum + ".png");
      ImageIO.write(m_PUImage, "png", outputfile);
      final Color transparent = new Color(0, 0, 0, 0);
      m_g2d.setColor(transparent);
      m_g2d.setComposite(AlphaComposite.Src);
      m_g2d.fill(new Rectangle2D.Float(0, 0, 600, 600));
    }
  }
}
