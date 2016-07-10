package games.strategy.triplea.image;

import games.strategy.debug.ClientLogger;

import java.awt.Image;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * We keep a soft reference to the image to allow it to be garbage collected.
 * Also, the image may not have finished watching when we are created, but the
 * getImage method ensures that the image will be loaded before returning.
 */
class ImageRef {
  public static final ReferenceQueue<Image> s_referenceQueue = new ReferenceQueue<>();
  public static final Logger s_logger = Logger.getLogger(ImageRef.class.getName());
  private static final AtomicInteger s_imageCount = new AtomicInteger();

  static {
    final Runnable r = new Runnable() {
      @Override
      public void run() {
        while (true) {
          try {
            s_referenceQueue.remove();
            s_logger.finer("Removed soft reference image. Image count:" + s_imageCount.decrementAndGet());
          } catch (final InterruptedException e) {
            ClientLogger.logQuietly(e);
          }
        }
      }
    };
    final Thread t = new Thread(r, "Tile Image Factory Soft Reference Reclaimer");
    t.setDaemon(true);
    t.start();
  }

  private final Reference<Image> m_image;

  // private final Object m_hardRef;
  public ImageRef(final Image image) {
    m_image = new SoftReference<>(image, s_referenceQueue);
    // m_hardRef = image;
    s_logger.finer("Added soft reference image. Image count:" + s_imageCount.incrementAndGet());
  }

  public Image getImage() {
    return m_image.get();
  }

  public void clear() {
    m_image.enqueue();
    m_image.clear();
  }
}
