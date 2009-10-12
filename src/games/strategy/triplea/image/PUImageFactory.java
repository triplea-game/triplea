package games.strategy.triplea.image;

import java.awt.Image;

public class PUImageFactory extends ImageFactory
{
    public Image getPUImage(int value)
    {
        return getImage("PUs/" + value + ".png", false);
    }
    
}
