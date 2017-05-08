package tools.map.making;

import java.awt.Image;
import java.awt.image.ImageObserver;
import java.util.concurrent.CountDownLatch;

/**
 * Code originally contributed by "Thomas Carvin".
 */
public class ImageIoCompletionWatcher implements ImageObserver {
  // we countdown when we are done
  private final CountDownLatch countDownLatch = new CountDownLatch(1);

  public ImageIoCompletionWatcher() {}

  public void waitForCompletion() {
    try {
      countDownLatch.await();
    } catch (final InterruptedException e) {
      // Ignore interrupted exception
    }
  }

  @Override
  public boolean imageUpdate(final Image image, final int flags, final int x, final int y, final int width,
      final int height) {
    // wait for complete or error/abort
    if (((flags & ALLBITS) != 0) || ((flags & ABORT) != 0)) {
      countDownLatch.countDown();
      return false;
    }
    return true;
  }
}
