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

package games.strategy.net;

import java.util.Map;

/**
 * An IConnectionLogin responds to login challenges.<p>
 * 
 * An IConnectionLogin is generally paired with an ILoginValidator.  The
 * validator will send a challenge string, to which the IConnectionLogin will
 * respond with a key/value map of credentials.  The validator will then 
 * allow the login, or return an error message.<p>
 * 
 * 
 * @author sgb
 */
public interface IConnectionLogin
{
    
    /**
     * Get the properties to log in given the challenge Properties 
     */
    public Map<String,String> getProperties(Map<String,String> challengProperties);
    
    /**
     * A notification that the login failed.  The error message supplied should be shown to the user.
     */
    public void notifyFailedLogin(String message);

}
