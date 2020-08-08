package tools.map.making;

import java.awt.Image;
import java.awt.image.ImageObserver;
import java.util.concurrent.CountDownLatch;
import org.triplea.java.Interruptibles;

/** Code originally contributed by "Thomas Carvin". */
public class ImageIoCompletionWatcher implements ImageObserver {
  // we countdown when we are done
  private final CountDownLatch countDownLatch = new CountDownLatch(1);

  public void waitForCompletion() {
    Interruptibles.await(countDownLatch);
  }

  @Override
  public boolean imageUpdate(
      final Image image,
      final int flags,
      final int x,
      final int y,
      final int width,
      final int height) {
    // wait for complete or error/abort
    if (((flags & ALLBITS) != 0) || ((flags & ABORT) != 0)) {
      countDownLatch.countDown();
      return false;
    }
    return true;
  }
}
