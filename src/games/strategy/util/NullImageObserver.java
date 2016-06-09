package games.strategy.util;

import java.awt.Image;
import java.awt.image.ImageObserver;

public class NullImageObserver implements ImageObserver {
  public NullImageObserver() {}

  @Override
  public boolean imageUpdate(final Image image, final int flags, final int int2, final int int3, final int int4,
      final int int5) {
    return (flags & ALLBITS) == 0 || ((flags & ABORT) != 0);
  }
}
