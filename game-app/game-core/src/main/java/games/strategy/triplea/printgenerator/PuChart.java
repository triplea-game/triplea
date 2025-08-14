package games.strategy.triplea.printgenerator;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.triplea.Constants;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.collections.IntegerMap;

@Slf4j
class PuChart {
  static final int DRAW_ROWS = 6;
  static final int DRAW_COLS = 7;

  private Iterable<GamePlayer> players;
  private IntegerMap<GamePlayer> moneyMap;
  private int numPlayers;
  private GamePlayer[] playerArray;
  private Integer[] moneyArray;
  private Map<Integer, Integer> avoidMap;
  private final Font chartFont = new Font("Serif", Font.PLAIN, 12);
  private BufferedImage puImage;
  private Path outDir;

  private void initializeMap() {
    int count = 0;
    for (final GamePlayer currentPlayer : players) {
      int playerQuantityPu = currentPlayer.getResources().getQuantity(Constants.PUS);
      moneyMap.put(currentPlayer, playerQuantityPu);
      playerArray[count] = currentPlayer;
      moneyArray[count] = playerQuantityPu;
      count++;
    }
  }

  private void initializeAvoidMap() {
    for (int i = 0; i < numPlayers - 1; i++) {
      for (int j = i + 1; j < numPlayers; j++) {
        // i = firstPlayerMoney ; j = secondPlayerMoney
        if (moneyArray[i].equals(moneyArray[j])) {
          avoidMap.put(i, j);
        }
      }
    }
  }

  private void drawEllipseAndString(
      final Graphics2D g2d, final int x, final int y, final String string) {
    g2d.setFont(chartFont);
    g2d.draw(new Ellipse2D.Double(5 + 87.0 * x, 5 + 87.0 * y, 72, 72));
    final FontMetrics metrics = g2d.getFontMetrics();
    final int h = metrics.stringWidth(string) / 2;
    final int k = metrics.getHeight() / 2;
    g2d.drawString(string, 42 + 87 * x - h, 39 + 87 * y + k);
  }

  protected void gatherDataBeforeWriting(PrintGenerationData printData) {
    final GameState gameData = printData.getData();
    players = gameData.getPlayerList();
    moneyMap = new IntegerMap<>();
    numPlayers = gameData.getPlayerList().size();
    playerArray = new GamePlayer[numPlayers];
    moneyArray = new Integer[numPlayers];
    avoidMap = new HashMap<>();
    puImage = new BufferedImage(600, 600, BufferedImage.TYPE_INT_ARGB);
    outDir = printData.getOutDir();

    initializeMap();
    initializeAvoidMap();
  }

  private void drawImage(int numChartsNeeded) {
    final Graphics2D g2d = puImage.createGraphics();
    for (int i = 0; i < numChartsNeeded; i++) {
      g2d.setColor(Color.black);
      drawCountryNames(i, g2d);
      // Draw Ellipses and Numbers
      for (int j = 0; j < DRAW_ROWS; j++) {
        for (int k = 0; k < DRAW_COLS; k++) {
          final int numberInCircle = DRAW_COLS * DRAW_ROWS * i + DRAW_COLS * j + k;
          drawEllipseAndString(g2d, k, j, Integer.toString(numberInCircle));
        }
      }
    }
    g2d.setColor(Color.black);
    g2d.setComposite(AlphaComposite.Src);
    g2d.fill(new Rectangle2D.Float(0, 0, 600, 600));
  }

  private void drawCountryNames(int i, Graphics2D g2d) {
    for (int z = 0; z < playerArray.length; z++) {
      final int valMod42 = moneyArray[z] % 42;
      final int valModXDim = valMod42 % DRAW_COLS;
      final int valFloorXDim = valMod42 / DRAW_COLS;
      if (avoidMap.containsKey(z) && moneyArray[z] / 42 == i) {
        final FontMetrics metrics = g2d.getFontMetrics();
        final int width = metrics.stringWidth(playerArray[z].getName()) / 2;
        g2d.drawString(
            playerArray[z].getName(), 42 + 87 * valModXDim - width, 63 + 87 * valFloorXDim);
      } else if (avoidMap.containsValue(z) && moneyArray[z] / 42 == i) {
        final FontMetrics metrics = g2d.getFontMetrics();
        final int width = metrics.stringWidth(playerArray[z].getName()) / 2;
        g2d.drawString(
            playerArray[z].getName(), 42 + 87 * valModXDim - width, 30 + 87 * valFloorXDim);
      } else if (moneyArray[z] / 42 == i) {
        final FontMetrics metrics = g2d.getFontMetrics();
        final int width = metrics.stringWidth(playerArray[z].getName()) / 2;
        g2d.drawString(
            playerArray[z].getName(), 42 + 87 * valModXDim - width, 60 + 87 * valFloorXDim);
      }
    }
  }

  void saveToFile(final PrintGenerationData printData) {
    gatherDataBeforeWriting(printData);
    final int numChartsNeeded =
        (int) Math.ceil(((double) moneyMap.totalValues()) / (DRAW_COLS * DRAW_ROWS));
    drawImage(numChartsNeeded);
    // Write to file
    final int firstNum = DRAW_COLS * DRAW_ROWS * numChartsNeeded;
    final int secondNum = DRAW_COLS * DRAW_ROWS * (numChartsNeeded + 1) - 1;
    final Path outFile = outDir.resolve("PuChart" + firstNum + "-" + secondNum + ".png");
    try {
      ImageIO.write(puImage, "png", outFile.toFile());
    } catch (final IOException e) {
      log.error("Failed to save print generation data to file {}", outFile, e);
    }
  }
}
