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

/**
    @author Georlge El-Haddad
    nekromancer@users.sourceforge.net
 
   Utility class for finding the local ip address of a machine with multiple network interfaces.
   
   This class will discard any InetAddress whose isLoobackAddresS() returns true.
   It will also discard any InetAddress whose isLinkLocalAddress() returns true.
 
   On most systems the IP addres it uses for internet communication will NOT be
   a LinkLocalAddress. Even if your system goes through a gateway, the standard
   192.168.0.1 address will be valid (not link local and not loopback). It is up
   to the user to tell his/her opponents the IP address of his/her gateway to
   connect to. And it is their responsibility to make sure they have port forwarding
   and IP masquarading set properly. TripleA will be bound to their local address
   and all packets will be routed through the gateway. Opponents will be bound to the
   gateway address. In essence it should all work.
   
   IF the game is run on the system that is acting as the dedicated gateway, many IPs 
   will be found as valid. The 1st IP that will be detected will be used. According to
   some tests, the 1st ip tends to be the IP used by the gateway to connect to the net.
   This means that TripleA will still work.
  
 */

import java.util.*;
import java.net.*;

public class IPFinder {

    public static InetAddress findInetAddress() throws SocketException {

    Enumeration e = NetworkInterface.getNetworkInterfaces();

    while (e.hasMoreElements()) {
        NetworkInterface netface = (NetworkInterface)e.nextElement();
        Enumeration e2 = netface.getInetAddresses();

            while (e2.hasMoreElements()) {
                InetAddress ip = (InetAddress) e2.nextElement();

                if( ! ip.isLoopbackAddress()) {
                    //Un-comment for debugging.
                    //System.out.println("Addres = "+ip.getHostAddress());
                    //System.out.println("is wild card address  = "+ip.isAnyLocalAddress());
                    //System.out.println("is link address       = "+ip.isLinkLocalAddress());
                    //System.out.println();

                    if ( ! ip.isLinkLocalAddress()) {
                        //Un-comment for debugging.
                        //System.out.println("Using IP = "+ip.getHostAddress());
                        return ip;
                    }//if

                }//if

            } //while2

        } //while1

        return null;   //or maybe "return InetAddress.getHostAddress()   for default ?

    }//end findInetAddress()

}//end class
