package games.strategy.ui;

import java.awt.image.*;
import java.awt.*;


public class ImageIoCompletionWatcher extends Thread implements ImageObserver
{

  private boolean m_started = false;
  private boolean m_complete = false;

  public void runUntilOpComplete()
  {
    if (!m_started)
    {
      this.start();
      m_started = true;
    }
  }

  public boolean isComplete()
  {
    return m_complete;
  }

  public void run()
  {

    // just hang out until the draw is complete
    waitForDrawComplete();

  }

  public boolean imageUpdate(Image image, int flags, int x, int y, int width,
                             int height)
  {

    // wait for complete or error/abort
    if ( ( (flags & ALLBITS) != 0) || ( (flags & ABORT) != 0))
    {
      notifyDrawComplete();
      return false;
    }

    return true;

  }

  private synchronized void waitForDrawComplete()
  {

    if (!m_complete)
    {
      try
      {
        wait();
      }
      catch (InterruptedException ie)
      {}
    }
  }

  private synchronized void notifyDrawComplete()
  {
    m_complete = true;
    notifyAll();
  }

}
