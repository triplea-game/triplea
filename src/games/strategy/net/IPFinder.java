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

	/**
	   We iterate through an enumeration of network interfaces on the machine
	   and picks the first IP that is not a loopback and not a link local.
	   In the case of IRIX computers connected on a LAN through a central
	   gateway running java off a telnet session will result in a null
	   network interface (patched below).

	   @exception java.net.SocketException       required by InetAddress
	   @exception java.net.UnknownHostException  required for getLocalHost()

	   @return    java.net.InetAddress           the ip address to use
	*/
	public static InetAddress findInetAddress() throws SocketException, UnknownHostException
	{
		Enumeration enum = NetworkInterface.getNetworkInterfaces();

		// Test if null, no point taking a performance hit by
		// letting the JVM check for a NullPointerException.
		if(enum == null) {
			InetAddress ip1 = InetAddress.getLocalHost();
			return ip1;
		}
		else {
			while (enum.hasMoreElements()) {
				NetworkInterface netface = (NetworkInterface)enum.nextElement();
				Enumeration enum2 = netface.getInetAddresses();
				while (enum2.hasMoreElements()) {
					InetAddress ip2 = (InetAddress) enum2.nextElement();
					if(! ip2.isLoopbackAddress()) {
					if(! ip2.isLinkLocalAddress()) {
						return ip2;
					}
					}
				}
			}
			//return null  <-- old code never throw null
			//If all else fails we return the localhost
			InetAddress ip3 = InetAddress.getLocalHost();
			return ip3;
		}//else

	}//end static findInetAddress()

}//end class IPFinder
