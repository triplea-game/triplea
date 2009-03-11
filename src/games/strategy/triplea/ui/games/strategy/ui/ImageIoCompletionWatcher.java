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
import java.util.concurrent.CountDownLatch;

/**
 * Code originally contributed by "Thomas Carvin"
 */

public class ImageIoCompletionWatcher implements ImageObserver
{
    //we countdown when we are done
    private final CountDownLatch m_countDownLatch = new CountDownLatch(1);

    public ImageIoCompletionWatcher()
    {
    }
    
    public void waitForCompletion()
    {
        try
        {
            m_countDownLatch.await();
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    public boolean imageUpdate(Image image, int flags, int x, int y, int width, int height)
    {
        // wait for complete or error/abort
        if (((flags & ALLBITS) != 0) || ((flags & ABORT) != 0))
        {
            m_countDownLatch.countDown();
            return false;
        }

        return true;

    }
}