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

public class ImageIoCompletionWatcher implements ImageObserver
{
  private boolean m_complete = false;
  private final Object m_lock = new Object();

  public void waitForCompletion()
  {
    synchronized(m_lock)
    {
      if (!m_complete)
      {
        try
        {
          m_lock.wait();
        }
        catch (InterruptedException ie)
        {}
      }
    }
  }

  public boolean imageUpdate(Image image, int flags, int x, int y, int width,
                             int height)
  {

    // wait for complete or error/abort
    if ( ( (flags & ALLBITS) != 0) || ( (flags & ABORT) != 0))
    {
      synchronized(m_lock)
      {
        m_complete = true;
        m_lock.notifyAll();
      }
      return false;
    }
    
    return true;

  }
}
