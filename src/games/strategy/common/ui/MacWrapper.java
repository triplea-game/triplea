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

package games.strategy.common.ui;

import javax.swing.JEditorPane;
import javax.swing.JOptionPane;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;


/**
 * Utility class to wrap Mac OS X-specific shutdown handler.<p>
 * 
 * Based on TripleA code.<p>
 * 
 * Needs AppleJavaExtensions.jar to compile on non-Mac platform.
 * 
 * @author Lane Schwartz
 * @see http://developer.apple.com/samplecode/AppleJavaExtensions/index.html
 */
public class MacWrapper
{
    private static MainGameFrame s_shutdownFrame;
    private static Application application = new Application();     
        
    static
    {
        application.addApplicationListener(
                new ApplicationAdapter() {
                    public void handleQuit(ApplicationEvent event) 
                    {   
                        if(s_shutdownFrame != null)
                            s_shutdownFrame.shutdown();
                        else 
                            System.exit(0);   
                    }});
    }
    
    //keep this in its own class, otherwise we get a no class def error when 
    //we try to load the game and the stubs arent in the classpath
    //i think the java validator triggers this
    public static void registerMacShutdownHandler(final MainGameFrame frame)
    {
        s_shutdownFrame = frame;
    }
    
    
    public static void unregisterShutdownHandler()
    {
        s_shutdownFrame = null;       
    }
    
    public static void addApplicationWrapper(final MainGameFrame frame, final JEditorPane editorPane) 
    {
        Application.getApplication().addApplicationListener(new ApplicationAdapter()
        {
            public void handleAbout(ApplicationEvent event)
            {
                event.setHandled(true); // otherwise the default About menu will still show appear
    
                JOptionPane.showMessageDialog(frame, editorPane, "About " + frame.getGame().getData().getGameName(), JOptionPane.PLAIN_MESSAGE);
            }
        });
    }
}
