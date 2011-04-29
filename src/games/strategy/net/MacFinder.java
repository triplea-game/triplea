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
        catch (Exception ex)
        {
            ex.printStackTrace();
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
        catch (Exception ex)
        {
            ex.printStackTrace();
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
            if (results != null && results.trim().length() > 0)
            {
                int macStartIndex = results.indexOf("-") - 2;
                String rawMac = results.substring(macStartIndex, 17);
                if (rawMac != null)
                {
                    String mac = rawMac.replace("-", ".");
                    if(isMacValid(mac))
                        return mac;
                }
            }
        }
        catch (Exception ex)
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
            if (results != null && results.trim().length() > 0)
            {
                int macStartIndex = results.indexOf("Physical Address. . . . . . . . . : ") + 36;
                String rawMac = results.substring(macStartIndex, 17);
                if (rawMac != null)
                {
                    String mac = rawMac.replace("-", ".");
                    if(isMacValid(mac))
                        return mac;
                }
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        //Next, try to get the mac address by calling the 'ifconfig /a' app that exists in Linux and possibly others.
        /*...
        eth0      Link encap:Ethernet HWaddr 00:08:C7:1B:8C:02
        ...*/
        try
        {
            String results = executeCommandAndGetResults("ifconfig -a");
            if (results != null && results.trim().length() > 0)
            {
                int macStartIndex = results.indexOf("Ethernet HWaddr ") + 16;
                String rawMac = results.substring(macStartIndex, 17);
                if (rawMac != null)
                {
                    String mac = rawMac.replace(":", ".");
                    if(isMacValid(mac))
                        return mac;
                }
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        //Next, try to get the mac address by calling the '/sbin/ifconfig /a' app that exists in Linux and possibly others.
        /*...
        eth0      Link encap:Ethernet HWaddr 00:08:C7:1B:8C:02
        ...*/
        try
        {
            String results = executeCommandAndGetResults("/sbin/ifconfig -a");
            if (results != null && results.trim().length() > 0)
            {
                int macStartIndex = results.indexOf("Ethernet HWaddr ") + 16;
                String rawMac = results.substring(macStartIndex, 17);
                if (rawMac != null)
                {
                    String mac = rawMac.replace(":", ".");
                    if(isMacValid(mac))
                        return mac;
                }
            }
        }
        catch (Exception ex)
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
        for(char ch : chars)
        {
            if(ch == '.')
                periodCount++;
            if(ch != '.' && ch != '0')
                nonZeroNumberCount++;
        }
        if(periodCount != 5)
            return false;
        if(nonZeroNumberCount == 0)
            return false;
        return true;
    }
}
