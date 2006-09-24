package games.strategy.triplea.ui;

@SuppressWarnings("deprecation")
public class MacWrapper
{
    private static TripleAFrame s_shutdownFrame;
    
    static
    {
        com.apple.mrj.MRJApplicationUtils.registerQuitHandler(new com.apple.mrj.MRJQuitHandler()
        {
           public void handleQuit()
           {
               if(s_shutdownFrame != null)
                   s_shutdownFrame.shutdown();
               else
                   System.exit(0);
           }
        });
    }
    
    //keep this in its own class, otherwise we get a no class def error when 
    //we try to load the game and the stubs arent in the classpath
    //i think the java validator triggers this
    public static void registerMacShutdownHandler(final TripleAFrame frame)
    {
        s_shutdownFrame = frame;
    }
    
    
    public static void unregisterShutdownHandler()
    {

        s_shutdownFrame = null;
        
    }
}
