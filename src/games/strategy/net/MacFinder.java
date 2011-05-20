/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package games.strategy.net;

import games.strategy.util.MD5Crypt;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 *
 * @author Stephen
 */
public class MacFinder
{
    /**
     * Should result in something like this: $1$MH$345ntXD4G3AKpAeHZdaGe3
     * @return
     */
	public static String GetHashedMacAddress()
    {
        String mac = GetMacAddress();
        if (mac == null)
        	throw new IllegalArgumentException("You have an invalid MAC address! (Or your Java is out of date, Or TripleA simply can't find your mac address)");
        return MD5Crypt.crypt(mac, "MH");
    }
    private static String GetMacAddress()
    {
        //We must try different methods of obtaining the mac address because not all the methods work on each system, and if we can't obtain the mac, we can't login to the lobby

        //First, try to get the mac address of the local host network interface
        try
        {
            InetAddress address = InetAddress.getLocalHost();
            NetworkInterface localHostNI = NetworkInterface.getByInetAddress(address);
            if (localHostNI != null)
            {
                byte[] rawMac = localHostNI.getHardwareAddress();
                String mac = convertMacBytesToString(rawMac);
                if (isMacValid(mac))
                    return mac;
            }
        }
        catch (Throwable ex) //Older java's don't have the getHardwareAddress method, so we catch not only Throwable->Exception's but all Throwable's, including Throwable->Error. (NoSuchMethodError is otherwise thrown)
        {
        	System.out.println("Your Java is out of date!");
        }

        //Next, try to get the mac address of the first network interfaces that has an accessible mac address
        try
        {
            Enumeration<NetworkInterface> niIter = NetworkInterface.getNetworkInterfaces();
            while (niIter.hasMoreElements())
            {
                NetworkInterface ni = niIter.nextElement();
                byte[] rawMac = ni.getHardwareAddress();
                String mac = convertMacBytesToString(rawMac);
                if (isMacValid(mac))
                    return mac;
            }
        }
        catch (Throwable ex) //Older java's don't have the getHardwareAddress method, so we catch not only Throwable->Exception's but all Throwable's, including Throwable->Error. (NoSuchMethodError is otherwise thrown)
        {
        	System.out.println("Your Java is out of date!");
        }

        //Next, try to get the mac address by calling the 'getmac' app that exists in Windows, Mac, and possibly others.
        /*
        Physical Address    Transport Name
        =================== ==========================================================
        00-1F-C6-F9-EC-E8   \Device\Tcpip_{99F55DF7-8C43-464C-A8A9-FA3F847467CB}
        */
        try
        {
            String results = executeCommandAndGetResults("getmac");
            while (results != null && results.trim().length() > 0)
            {
                int macStartIndex = Math.max(0, results.indexOf("-") - 2);
                String rawMac = results.substring(macStartIndex, Math.min(17 + macStartIndex, results.length()));
                if (rawMac != null)
                {
                    String mac = rawMac.replace("-", ".");
                    if(isMacValid(mac))
                        return mac;
                }
                results = results.substring(Math.min(17 + macStartIndex, results.length()));
            }
        }
        catch (Throwable ex)
        {
        	ex.printStackTrace();
        }

        //Next, try to get the mac address by calling the 'ipconfig /all' app that exists in Windows and possibly others.
        /*...
        Physical Address. . . . . . . . . : 00-1C-D3-F8-DC-E8
        ...*/
        try
        {
            String results = executeCommandAndGetResults("ipconfig /all");
            while (results != null && results.trim().length() > 0)
            {
                int macStartIndex = Math.max(0, Math.min(results.length()-1, results.indexOf("Physical Address. . . . . . . . . : ") + 36));
                String rawMac = results.substring(macStartIndex, Math.min(17 + macStartIndex, results.length()));
                if (rawMac != null)
                {
                    String mac = rawMac.replace("-", ".");
                    if(isMacValid(mac))
                        return mac;
                }
                results = results.substring(Math.min(17 + macStartIndex, results.length()));
            }
        }
        catch (Throwable ex)
        {
        	ex.printStackTrace();
        }

        //Next, try to get the mac address by calling the 'ifconfig /a' app that exists in Linux and possibly others. May have 1 or 2 spaces between Ethernet and HWaddr, and may be wireless instead of ethernet.
        /*...
        eth0      Link encap:Ethernet HWaddr 00:08:C7:1B:8C:02
        ...*/
        try
        {
            String results = executeCommandAndGetResults("ifconfig -a");
            while (results != null && results.trim().length() > 0)
            {
                int macStartIndex = Math.max(0, Math.min(results.length()-1, results.indexOf("HWaddr ") + 7));
                String rawMac = results.substring(macStartIndex, Math.min(17 + macStartIndex, results.length()));
                if (rawMac != null)
                {
                    String mac = rawMac.replace(":", ".");
                    if(isMacValid(mac))
                        return mac;
                }
                results = results.substring(Math.min(17 + macStartIndex, results.length()));
            }
        }
        catch (Throwable ex)
        {
        	ex.printStackTrace();
        }

        //Next, try to get the mac address by calling the '/sbin/ifconfig /a' app that exists in Linux and possibly others. May have 1 or 2 spaces between Ethernet and HWaddr, and may be wireless instead of ethernet.
        /*...
        eth0      Link encap:Ethernet HWaddr 00:08:C7:1B:8C:02
        ...*/
        try
        {
            String results = executeCommandAndGetResults("/sbin/ifconfig -a");
            while (results != null && results.trim().length() > 0)
            {
                int macStartIndex = Math.max(0, Math.min(results.length()-1, results.indexOf("HWaddr ") + 7));
                String rawMac = results.substring(macStartIndex, Math.min(17 + macStartIndex, results.length()));
                if (rawMac != null)
                {
                    String mac = rawMac.replace(":", ".");
                    if(isMacValid(mac))
                        return mac;
                }
                results = results.substring(Math.min(17 + macStartIndex, results.length()));
            }
        }
        catch (Throwable ex)
        {
        	ex.printStackTrace();
        }
        
        //Next try to get the mac address on Mac OS X, Solaris, and SunOS. 
        //MacOSX, Solaris, SunOS all use "ifconfig -a", while FreeBSD uses "dmesg", while HPUX uses "lanscan".  http://www.opentutorial.com/Find_your_mac_address
        //Fun stuff: Solaris, SunOS may or may not strip off any leading zeros in the address, while RedHat and Fedora require root to do ifconfig
        /*
        MacOSX:
        # ifconfig
        inet 127.0.0.1 netmask 0xff000000
		inet6 ::1 prefixlen 128
		inet6 fe80::1%lo0 prefixlen 64 scopeid 0x1
		inet 10.25.10.51 netmask 0xfffffc00 broadcast 10.25.11.255
		ether 00:0d:93:70:ed:44
		
        Solaris & SunOS:
        # ifconfig -a
		le0: flags=863<UP,BROADCAST,NOTRAILERS,RUNNING,MULTICAST> mtu 1500
		       inet 131.225.80.209 netmask fffff800 broadcast 131.225.87.255
		       ether 8:0:20:10:d2:ae 
         */
        try
        {
            String results = executeCommandAndGetResults("ifconfig -a");
            while (results != null && results.trim().length() > 0)
            {
                int macStartIndex = Math.max(0, Math.min(results.length()-1, results.indexOf("ether ") + 6));
                String rawMac = results.substring(macStartIndex, Math.min(17 + macStartIndex, results.length()));
                if (rawMac != null)
                {
                    String mac = rawMac.replace(":", ".");
                    if(isMacValid(mac))
                        return mac;
                }
                if (rawMac != null)
                {
                    rawMac = "0" + rawMac; //there could be other zeros mid-mac....
                    rawMac = rawMac.substring(0, Math.min(17, rawMac.length()));
                    String mac = rawMac.replace(":", ".");
                    if(isMacValid(mac))
                        return mac;
                }
                results = results.substring(Math.min(17 + macStartIndex, results.length()));
            }
        }
        catch (Throwable ex)
        {
        	ex.printStackTrace();
        }
        try
        {
            String results = executeCommandAndGetResults("/sbin/ifconfig -a");
            while (results != null && results.trim().length() > 0)
            {
                int macStartIndex = Math.max(0, Math.min(results.length()-1, results.indexOf("ether ") + 6));
                String rawMac = results.substring(macStartIndex, Math.min(17 + macStartIndex, results.length()));
                if (rawMac != null)
                {
                    String mac = rawMac.replace(":", ".");
                    if(isMacValid(mac))
                        return mac;
                }
                if (rawMac != null)
                {
                    rawMac = "0" + rawMac; //there could be other zeros mid-mac....
                    rawMac = rawMac.substring(0, Math.min(17, rawMac.length()));
                    String mac = rawMac.replace(":", ".");
                    if(isMacValid(mac))
                        return mac;
                }
                results = results.substring(Math.min(17 + macStartIndex, results.length()));
            }
        }
        catch (Throwable ex)
        {
        	ex.printStackTrace();
        }

        return null;
    }
    private static String executeCommandAndGetResults(String command)
    {
        Process p = null;
        try
        {
            p = new ProcessBuilder(command).start();
        }
        catch (Exception ex)
        {
            try
            {
                p = Runtime.getRuntime().exec(command);
            }
            catch (IOException ex2)
            {
            }
        }
        if(p == null)
            return null;
        try
        {
            StringBuilder builder = new StringBuilder();
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while (true)
            {
                try
                {
                    String line = in.readLine();
                    if (line == null)
                        break;
                    builder.append(line).append("\r\n");
                }
                catch (IOException ex)
                {
                    break;
                }
            }
            in.close();
            return builder.toString();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            return null;
        }
    }
    private static String convertMacBytesToString(byte[] mac)
    {
    	if (mac == null)
    		return null;
    	
        StringBuilder macStringBuilder = new StringBuilder();
        //Extract each array of mac address and convert it to the following format 00.1E.F3.C8.FC.E6
        for (int i = 0; i < mac.length; i++)
        {
            if (i != 0)
                macStringBuilder.append(".");
            String hex = String.format("%02X", mac[i]);
            macStringBuilder.append(hex);
        }
        return macStringBuilder.toString();
    }
    public static boolean isMacValid(String mac)
    {
        if(mac == null)
            return false;
        if(mac.length() != 17)
            return false;
        if (!mac.contains("."))
            return false;
        if(!mac.matches("[0-9A-F.]+"))
            return false;
        char[] chars = mac.toCharArray();
        int periodCount = 0;
        int nonZeroNumberCount = 0;
        int i = 1;
        for(char ch : chars)
        {
        	if(ch == '.' && (i%3 != 0))
        		return false;
            if(ch == '.')
                periodCount++;
            if(ch != '.' && ch != '0')
                nonZeroNumberCount++;
            i++;
        }
        if(periodCount != 5)
            return false;
        if(nonZeroNumberCount == 0)
            return false;
        return true;
    }
}
