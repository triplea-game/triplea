package games.strategy.triplea.image;

import java.awt.Image;

public class PuImageFactory extends ImageFactory {
  public Image getPuImage(final int value) {
    return getImage("PUs/" + value + ".png", false);
  }
}
