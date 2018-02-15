package games.strategy.triplea.image;

import java.awt.Image;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

import games.strategy.util.Util;

/**
 * We keep a soft reference to the image to allow it to be garbage collected.
 * Also, the image may not have finished watching when we are created, but the
 * getImage method ensures that the image will be loaded before returning.
 */
class ImageRef {
  private static final ReferenceQueue<Image> referenceQueue = new ReferenceQueue<>();

  static {
    Util.createDaemonThread(() -> {
      while (true) {
        try {
          referenceQueue.remove();
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }, "Tile Image Factory Soft Reference Reclaimer").start();
  }

  private final Reference<Image> image;

  public ImageRef(final Image image) {
    this.image = new SoftReference<>(image, referenceQueue);
  }

  public Image getImage() {
    return image.get();
  }

  public void clear() {
    image.enqueue();
    image.clear();
  }
}
