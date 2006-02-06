package games.strategy.engine.framework;

import games.strategy.debug.Console;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.triplea.ui.ErrorHandler;

import java.util.logging.LogManager;

import javax.swing.UIManager;

public class GameRunner2
{
    public static void main(String[] args)
    {
        setupLogging();
        
        Console.getConsole().displayStandardError();
        Console.getConsole().displayStandardOutput();

        System.setProperty("sun.awt.exception.handler", ErrorHandler.class.getName());
        
        setupLookAndFeel();

        showMainFrame();
    }

    private static void showMainFrame()
    {
        MainFrame frame = new MainFrame();
        frame.setVisible(true);
    }

    private static void setupLookAndFeel()
    {
        try
        {
            //macs are already beautiful
            if (!GameRunner.isMac())
            {
                com.jgoodies.looks.plastic.PlasticLookAndFeel.setTabStyle(com.jgoodies.looks.plastic.PlasticLookAndFeel.TAB_STYLE_METAL_VALUE);
                //com.jgoodies.plaf.plastic.PlasticXPLookAndFeel.setTabStyle(com.jgoodies.plaf.plastic.PlasticLookAndFeel.TAB_STYLE_METAL_VALUE);
                UIManager.setLookAndFeel(new com.jgoodies.looks.plastic.PlasticXPLookAndFeel());
                com.jgoodies.looks.Options.setPopupDropShadowEnabled(true);

            }
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
    
    private static void setupLogging()
    {
        //setup logging to read our logging.properties
        try
        {
            LogManager.getLogManager().readConfiguration(ClassLoader.getSystemResourceAsStream("logging.properties"));
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
