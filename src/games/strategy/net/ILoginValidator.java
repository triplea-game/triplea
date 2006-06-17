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
 */

package games.strategy.net;

import java.net.SocketAddress;
import java.util.Map;

/**
 * Code to validate a login attempt.  <p>
 * 
 * @see games.strategy.net.IConnectionLogin
 * 
 * @author sgb
 */
public interface ILoginValidator
{
    
    /**
     * 
     * The challenge properties to send to the client.  The client will be sent the challenge string,
     * and expected to return a properties object to validate its connection.
     */
    public Map<String,String> getChallengeProperties(String userName, SocketAddress remoteAddress);
    
    /**
     * @param propertiesReadFromClient - client properties written by the client after receiving the challange string.
     * @param remoteAddress - the remote adress 
     * @param clientName - the user name given by the client
     * 
     *
     * @return - null if the attempt was successful, an error message otherwise
     */
    public String verifyConnection(Map<String,String> propertiesReadFromClient, String clientName, SocketAddress remoteAddress);
    
    
}
