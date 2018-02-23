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
import java.util.Map;

import javax.imageio.ImageIO;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.triplea.Constants;
import games.strategy.util.IntegerMap;

class PuChart {
  private final Iterable<PlayerID> players;
  private final IntegerMap<PlayerID> moneyMap;
  private final int numPlayers;
  private final PlayerID[] playerArray;
  private final Integer[] moneyArray;
  private final Map<Integer, Integer> avoidMap;
  private final Font chartFont = new Font("Serif", Font.PLAIN, 12);
  private final BufferedImage puImage;
  private final Graphics2D g2d;
  private final File outDir;

  PuChart(final PrintGenerationData printData) {
    final GameData gameData = printData.getData();
    players = gameData.getPlayerList();
    moneyMap = new IntegerMap<>();
    numPlayers = gameData.getPlayerList().size();
    playerArray = new PlayerID[numPlayers];
    moneyArray = new Integer[numPlayers];
    avoidMap = new HashMap<>();
    puImage = new BufferedImage(600, 600, BufferedImage.TYPE_INT_ARGB);
    g2d = puImage.createGraphics();
    outDir = printData.getOutDir();
  }

  private void initializeMap() {
    int count = 0;
    for (final PlayerID currentPlayer : players) {
      moneyMap.put(currentPlayer, currentPlayer.getResources().getQuantity(Constants.PUS));
      playerArray[count] = currentPlayer;
      moneyArray[count] = currentPlayer.getResources().getQuantity(Constants.PUS);
      count++;
    }
  }

  private void initializeAvoidMap() {
    for (int i = 0; i < (numPlayers - 1); i++) {
      for (int j = i + 1; j < numPlayers; j++) {
        // i = firstPlayerMoney ; j = secondPlayerMoney
        if (moneyArray[i].equals(moneyArray[j])) {
          // avoidMap.put(playerArray[i], playerArray[j]);
          avoidMap.put(i, j);
        }
      }
    }
  }

  private void drawEllipseAndString(final int x, final int y, final String string) {
    g2d.setFont(chartFont);
    g2d.draw(new Ellipse2D.Double(5 + (87 * x), 5 + (87 * y), 72, 72));
    final FontMetrics metrics = g2d.getFontMetrics();
    final int h = metrics.stringWidth(string) / 2;
    final int k = metrics.getHeight() / 2;
    g2d.drawString(string, (42 + (87 * x)) - h, 39 + (87 * y) + k);
  }

  void saveToFile() throws IOException {
    initializeMap();
    initializeAvoidMap();
    final int rows = 6;
    final int cols = 7;
    final int numChartsNeeded = (int) Math.ceil(((double) moneyMap.totalValues()) / (cols * rows));
    for (int i = 0; i < numChartsNeeded; i++) {
      g2d.setColor(Color.black);
      // Draw Country Names
      for (int z = 0; z < playerArray.length; z++) {
        final int valMod42 = moneyArray[z] % 42;
        final int valModXDim = valMod42 % cols;
        final int valFloorXDim = valMod42 / cols;
        if (avoidMap.containsKey(z) && ((moneyArray[z] / 42) == i)) {
          final FontMetrics metrics = g2d.getFontMetrics();
          final int width = metrics.stringWidth(playerArray[z].getName()) / 2;
          g2d.drawString(playerArray[z].getName(), (42 + (87 * valModXDim)) - width, 63 + (87 * valFloorXDim));
        } else if (avoidMap.containsValue(z) && ((moneyArray[z] / 42) == i)) {
          final FontMetrics metrics = g2d.getFontMetrics();
          final int width = metrics.stringWidth(playerArray[z].getName()) / 2;
          g2d.drawString(playerArray[z].getName(), (42 + (87 * valModXDim)) - width, 30 + (87 * valFloorXDim));
        } else if ((moneyArray[z] / 42) == i) {
          final FontMetrics metrics = g2d.getFontMetrics();
          final int width = metrics.stringWidth(playerArray[z].getName()) / 2;
          g2d.drawString(playerArray[z].getName(), (42 + (87 * valModXDim)) - width, 60 + (87 * valFloorXDim));
        }
      }
      // Draw Ellipses and Numbers
      for (int j = 0; j < rows; j++) {
        for (int k = 0; k < cols; k++) {
          final int numberincircle = (cols * rows * i) + (cols * j) + k;
          final String string = "" + numberincircle;
          drawEllipseAndString(k, j, string);
        }
      }
      // Write to file
      final int firstnum = cols * rows * i;
      final int secondnum = (cols * rows * (i + 1)) - 1;
      final File outputfile = new File(outDir, "PUchart" + firstnum + "-" + secondnum + ".png");
      ImageIO.write(puImage, "png", outputfile);
      final Color transparent = new Color(0, 0, 0, 0);
      g2d.setColor(transparent);
      g2d.setComposite(AlphaComposite.Src);
      g2d.fill(new Rectangle2D.Float(0, 0, 600, 600));
    }
  }
}
