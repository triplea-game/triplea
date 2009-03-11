package games.strategy.triplea.image;

import java.awt.Image;

public class IPCImageFactory extends ImageFactory
{
    public Image getIpcImage(int value)
    {
        return getImage("ipcs/" + value + ".png", false);
    }
    
}
