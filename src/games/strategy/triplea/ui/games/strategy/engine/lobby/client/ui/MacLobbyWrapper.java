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

package games.strategy.engine.lobby.client.ui;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

/**
 * TODO This class should be merged with games.strategy.common.ui.MacWrapper.
 * 
 */
public class MacLobbyWrapper
{
    
    //keep this in its own class, otherwise we get a no class def error when 
    //we try to load the game and the stubs arent in the classpath
    //i think the java validator triggers this
    
    public static void registerMacShutdownHandler(final LobbyFrame frame)
    {
        Application application = new Application();
        
        application.addApplicationListener(
                new ApplicationAdapter() {
                    public void handleQuit(ApplicationEvent event) 
                    {   
                        if(frame != null)
                            frame.shutdown();
                        else 
                            System.exit(0);   
                    }});
        
    }


}
