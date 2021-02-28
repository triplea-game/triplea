package games.strategy.triplea.image;

import java.awt.Image;

/**
 * A factory for creating territory production images. These images are overlaid on a territory to
 * indicate how many PUs the territory produces each turn.
 */
public class PuImageFactory extends ImageFactory {
  public Image getPuImage(final int value) {
    return getImage("PUs/" + value + ".png", false);
  }
}
