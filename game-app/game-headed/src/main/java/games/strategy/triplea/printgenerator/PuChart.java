package games.strategy.triplea.printgenerator;

import games.strategy.engine.data.GamePlayer;
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
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.collections.IntegerMap;

@Slf4j
class PuChart {
  public static final int IMAGE_WIDTH = 600;
  public static final int IMAGE_HEIGHT = 600;
  static final int DRAW_ROWS = 6;
  static final int DRAW_COLS = 7;
  static final int CIRCLES_PER_IMAGE = DRAW_COLS * DRAW_ROWS;
  private final IntegerMap<GamePlayer> moneyMap = new IntegerMap<>();
  private final Font chartFont = new Font("Serif", Font.PLAIN, 12);
  private List<GamePlayer> players; // list of players sorted by Pus
  private Path outDir;

  private static void clearImageGraphics(Graphics2D g2d) {
    final Color transparent = new Color(0, 0, 0, 0);
    g2d.setColor(transparent);
    g2d.setComposite(AlphaComposite.Src);
    g2d.fill(new Rectangle2D.Float(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT));
  }

  protected void gatherDataBeforeWriting(PrintGenerationData printData) {
    outDir = printData.getOutDir();
    players = printData.getData().getPlayerList().getPlayers();
    players.forEach(
        currentPlayer ->
            moneyMap.put(currentPlayer, currentPlayer.getResources().getQuantity(Constants.PUS)));
    players.sort(Comparator.comparing(moneyMap::getInt));
  }

  private void drawChartImagesAndSaveFiles() {
    final BufferedImage puImage =
        new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g2d = puImage.createGraphics();

    SortedPlayers sortedPlayers = new SortedPlayers(players);
    for (int imgCount = 0; !sortedPlayers.finished(); imgCount++) {
      g2d.setColor(Color.black);
      drawPlayerNames(sortedPlayers, imgCount, g2d);
      drawCirclesAndNumbers(imgCount, g2d);
      writeImageToFile(imgCount, puImage);
      clearImageGraphics(g2d);
    }
  }

  private void drawPlayerNames(SortedPlayers sortedPlayers, final int imgCount, Graphics2D g2d) {
    int samePuCount = 0;
    while (sortedPlayers.playerPuValue / 42 == imgCount) {
      final int valMod42 = sortedPlayers.playerPuValue % 42;
      final int valModXDim = valMod42 % DRAW_COLS;
      final int valFloorXDim = valMod42 / DRAW_COLS;
      int drawY =
          switch (samePuCount) {
            case 0 -> 61 + 87 * valFloorXDim; // underneath the PU number
            case 1 -> 32 + 87 * valFloorXDim; // above the PU number
            case 2 -> 20 + 87 * valFloorXDim; // above the PU number
            default -> 73 + 87 * valFloorXDim; // underneath the PU number
          };
      int lastPlayerPuValue = sortedPlayers.playerPuValue;
      if (samePuCount < 3) {
        drawString(g2d, sortedPlayers.currentPlayer.getName(), valModXDim, drawY);
        if (lastPlayerPuValue == sortedPlayers.getNextPlayerPu()) {
          samePuCount++;
          continue;
        }
      } else {
        drawStringFourAndMore(sortedPlayers, g2d, lastPlayerPuValue, valModXDim, drawY);
      }
      if (sortedPlayers.finished()) {
        return;
      }
      samePuCount = 0;
    }
  }

  private void drawStringFourAndMore(
      SortedPlayers sortedPlayers,
      Graphics2D g2d,
      int lastPlayerPuValue,
      int valModXDim,
      int drawY) {
    int countNotShown = 1;
    while (lastPlayerPuValue == sortedPlayers.getNextPlayerPu()) {
      countNotShown++;
    }
    if (countNotShown == 1) { // only one player more, draw this last players name
      drawString(g2d, sortedPlayers.currentPlayer.getName(), valModXDim, drawY);
    } else { // more than one player more, draw message about the number of players
      drawString(g2d, MessageFormat.format("{0} more players", countNotShown), valModXDim, drawY);
    }
  }

  private void drawString(Graphics2D g2d, String string, int valModXDim, int drawY) {
    final FontMetrics metrics = g2d.getFontMetrics();
    final int widthToStringCenter = metrics.stringWidth(string) / 2;
    int drawX = 42 + 87 * valModXDim - widthToStringCenter;
    g2d.drawString(string, drawX, drawY);
  }

  private void drawCirclesAndNumbers(int imgCount, Graphics2D g2d) {
    for (int j = 0; j < DRAW_ROWS; j++) {
      for (int k = 0; k < DRAW_COLS; k++) {
        final int numberInCircle = CIRCLES_PER_IMAGE * imgCount + DRAW_COLS * j + k;
        drawCircleAndString(g2d, k, j, Integer.toString(numberInCircle));
      }
    }
  }

  private void drawCircleAndString(
      final Graphics2D g2d, final int x, final int y, final String string) {
    g2d.setFont(chartFont);
    g2d.draw(new Ellipse2D.Double(5 + 87.0 * x, 5 + 87.0 * y, 72, 72));
    final FontMetrics metrics = g2d.getFontMetrics();
    final int h = metrics.stringWidth(string) / 2;
    final int k = metrics.getHeight() / 2;
    g2d.drawString(string, 42 + 87 * x - h, 39 + 87 * y + k);
  }

  private void writeImageToFile(int imgCount, BufferedImage puImage) {
    final int firstNum = CIRCLES_PER_IMAGE * imgCount;
    final int secondNum = firstNum + CIRCLES_PER_IMAGE - 1;
    final Path outFile = outDir.resolve("PuChart" + firstNum + "-" + secondNum + ".png");
    try {
      ImageIO.write(puImage, "png", outFile.toFile());
    } catch (final IOException e) {
      log.error("Failed to save print generation data to file {}", outFile, e);
    }
  }

  void saveToFiles(final PrintGenerationData printData) {
    gatherDataBeforeWriting(printData);
    drawChartImagesAndSaveFiles();
  }

  class SortedPlayers {

    final Iterator<GamePlayer> iteratorPlayer;
    GamePlayer currentPlayer;
    int playerPuValue;

    SortedPlayers(final List<GamePlayer> players) {
      iteratorPlayer = players.iterator();
      getNextPlayerPu();
    }

    boolean finished() {
      return (currentPlayer == null);
    }

    int getNextPlayerPu() {
      if (iteratorPlayer.hasNext()) {
        currentPlayer = iteratorPlayer.next();
        playerPuValue = moneyMap.getInt(currentPlayer);
      } else {
        currentPlayer = null;
        playerPuValue = -1;
      }
      return playerPuValue;
    }
  }
}
