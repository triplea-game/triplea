package games.strategy.ui;

/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */


import java.awt.image.*;
import java.awt.*;

/**
 * Code originally contributed by "Thomas Carvin"
 */

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
