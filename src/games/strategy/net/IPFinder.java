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
 *    @author Georlge El-Haddad
 *   nekromancer@users.sourceforge.net
 *
 *  Utility class for finding the local ip address of a machine with multiple network interfaces.
 *  returns the first ip adress that it finds that is not 127.0.0.1
 */



import java.util.*;
import java.net.*;

public class IPFinder
{

 public static InetAddress findInetAddress() throws SocketException
  {

   Enumeration e = NetworkInterface.getNetworkInterfaces();
   while (e.hasMoreElements())
   {
     NetworkInterface netface =
       (NetworkInterface) e.nextElement();

     Enumeration e2 = netface.getInetAddresses();
     while (e2.hasMoreElements())
     {
       InetAddress ip = (InetAddress) e2.nextElement();
       if (!ip.getHostAddress().equals("127.0.0.1"))
       {
         return ip;
       }

     } //while2

   } //while1
   return null;
 }


 }

