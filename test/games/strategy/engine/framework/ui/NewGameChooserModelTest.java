package games.strategy.engine.framework.ui;

import junit.framework.TestCase;

public class NewGameChooserModelTest extends TestCase
{

    
    public void testCreate()
    {
        NewGameChooserModel model = new NewGameChooserModel();
        assertTrue(model.size() + "", model.size() > 0);        
    }
}
