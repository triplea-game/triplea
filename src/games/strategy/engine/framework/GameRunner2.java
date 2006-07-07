package games.strategy.engine.framework;

import games.strategy.debug.Console;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.triplea.ui.ErrorHandler;

import java.util.logging.LogManager;

import javax.swing.*;


public class GameRunner2
{
    public static final String TRIPLEA_GAME_PROPERTY = "triplea.game";
    public static final String TRIPLEA_HOST_PROPERTY = "triplea.host";
    public static final String TRIPLEA_PORT_PROPERTY = "triplea.port";
    public static final String TRIPLEA_SERVER_PASSWORD_PROPERTY = "triplea.server.password";
    public static final String TRIPLEA_CLIENT_PROPERTY = "triplea.client";
    public static final String TRIPLEA_SERVER_PROPERTY = "triplea.server";
    public static final String TRIPLEA_NAME_PROPERTY = "triplea.name";
    public static final String TRIPLEA_STARTED = "triplea.started";

    //these properties are for games that should connect to the Lobby Server
    public static final String LOBBY_PORT = "triplea.lobby.port";
    public static final String LOBBY_HOST = "triplea.lobby.host";
    
    
    
    public static void main(String[] args)
    {
        setupLogging();
        
        Console.getConsole().displayStandardError();
        Console.getConsole().displayStandardOutput();

        System.setProperty("sun.awt.exception.handler", ErrorHandler.class.getName());
        
        setupLookAndFeel();

        handleCommandLineArgs(args);
        showMainFrame();
    }

    private static void showMainFrame()
    {
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                MainFrame frame = new MainFrame();        
                frame.start();
                frame.requestFocus();
                frame.toFront();

            }
        
        });
    }
    
    /**
     * Move command line arguments to System.properties
     */
    private static void handleCommandLineArgs(String[] args)
    {
        String[] properties = new String[] {TRIPLEA_SERVER_PROPERTY, TRIPLEA_CLIENT_PROPERTY, TRIPLEA_PORT_PROPERTY, TRIPLEA_HOST_PROPERTY, TRIPLEA_GAME_PROPERTY, TRIPLEA_NAME_PROPERTY, TRIPLEA_SERVER_PASSWORD_PROPERTY};
        
        
        //if only 1 arg, it must be the game name
        //find it
        //optionally, it may not start with the property name
        if(args.length == 1)
        {
            String value;
            if(args[0].startsWith(TRIPLEA_GAME_PROPERTY))
            {
                value = getValue( TRIPLEA_GAME_PROPERTY);
            }
            else
            {
                value = args[0];
            }
            System.setProperty(TRIPLEA_GAME_PROPERTY, value);
            return;
        }
        
        boolean usagePrinted = false;
        for(int argIndex = 0; argIndex < args.length; argIndex++)
        {
            boolean found = false;
            for(int propIndex = 0; propIndex < properties.length; propIndex++)
            {
                if(args[argIndex].startsWith(properties[propIndex]))
                {
                    String value = getValue(args[argIndex]);
                    System.getProperties().setProperty(properties[propIndex], value);
                    System.out.println(properties[propIndex] + ":" + value );
                    found = true;
                }
            }
            if(!found)
            {
                System.out.println("Unrecogized:" + args[argIndex]);
                if(!usagePrinted)
                {
                    usagePrinted = true;
                    usage();
                }
            }
        }
    }
    
    private static void usage()
    {
        System.out.println("Arguments\n" + 
                "   triplea.game=<FILE_NAME>\n" + 
                "   triplea.server=true\n" + 
                "   triplea.client=true\n" + 
                "   triplea.port=<PORT>\n" + 
                "   triplea.host=<HOST>\n" + 
                "   triplea.name=<PLAYER_NAME>\n" + 
                "\n" + 
                "if there is only one argument, and it does not start with triplea.game, the argument will be \n" + 
                "taken as the name of the file to load.\n" + 
                "\n" + 
                "Example\n" + 
                "   to start a game using the given file:\n" + 
                "\n" + 
                "   triplea /home/sgb/games/test.xml\n" + 
                "\n" + 
                "   or\n" + 
                "\n" + 
                "   triplea triplea.game=/home/sgb/games/test.xml\n" + 
                "\n" + 
                "   to connect to a remote host:\n" + 
                "\n" + 
                "   triplea triplea.client=true triplea.host=127.0.0.0 triplea.port=3300 triplea.name=Paul\n" + 
                "\n" + 
                "   to start a server with the given game\n" + 
                "\n" + 
                "   triplea triplea.game=/home/sgb/games/test.xml triplea.server=true triplea.port=3300 triplea.name=Allan" + 
                "\n" +
                "   to start a server, you can optionally password protect the game using triplea.server.password=foo" 
                );
    }
    
    private static String getValue(String arg)
    {
        int index = arg.indexOf('=');
        if(index == -1)
            return "";
        return arg.substring(index +1);
    }

    public static void setupLookAndFeel()
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
        } catch (Throwable ex)
        {
            ex.printStackTrace();
        }
    }
    
    public static void setupLogging()
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
