package games.strategy.net;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * nekromancer@users.sourceforge.net
 * Utility class for finding the local ip address of a machine with multiple network interfaces.
 * This class will discard any InetAddress whose isLoobackAddresS() returns true.
 * It will also discard any InetAddress whose isLinkLocalAddress() returns true.
 * On most systems the IP addres it uses for internet communication will NOT be
 * a LinkLocalAddress. Even if your system goes through a gateway, the standard
 * 192.168.0.1 address will be valid (not link local and not loopback). It is up
 * to the user to tell his/her opponents the IP address of his/her gateway to
 * connect to. And it is their responsibility to make sure they have port forwarding
 * and IP masquarading set properly. TripleA will be bound to their local address
 * and all packets will be routed through the gateway. Opponents will be bound to the
 * gateway address. In essence it should all work.
 * IF the game is run on the system that is acting as the dedicated gateway, many IPs
 * will be found as valid. The 1st IP that will be detected will be used. According to
 * some tests, the 1st ip tends to be the IP used by the gateway to connect to the net.
 * This means that TripleA will still work.
 */
public class IpFinder {
  /**
   * We iterate through an enumeration of network interfaces on the machine
   * and picks the first IP that is not a loopback and not a link local and not private.
   * In the case of IRIX computers connected on a LAN through a central
   * gateway running java off a telnet session will result in a null
   * network interface (patched below).
   *
   * @exception java.net.SocketException
   *            required by InetAddress
   * @exception java.net.UnknownHostException
   *            required for getLocalHost()
   * @return java.net.InetAddress the ip address to use
   */
  public static InetAddress findInetAddress() throws SocketException, UnknownHostException {
    final Enumeration<NetworkInterface> enum1 = NetworkInterface.getNetworkInterfaces();
    // Test if null, no point taking a performance hit by
    // letting the JVM check for a NullPointerException.
    if (enum1 == null) {
      final InetAddress ip1 = InetAddress.getLocalHost();
      return ip1;
    }
    final List<InetAddress> allButLoopback = new ArrayList<>();
    while (enum1.hasMoreElements()) {
      final NetworkInterface netface = enum1.nextElement();
      final Enumeration<InetAddress> enum2 = netface.getInetAddresses();
      while (enum2.hasMoreElements()) {
        final InetAddress ip2 = enum2.nextElement();
        if (!ip2.isLoopbackAddress()) {
          allButLoopback.add(ip2);
        }
      }
    }
    // try to find one that is not private and ip4
    for (final InetAddress address : allButLoopback) {
      if (address.getAddress().length == 4 && !address.isSiteLocalAddress()) {
        return address;
      }
    }
    // try to find one that is not private
    for (final InetAddress address : allButLoopback) {
      if (!address.isSiteLocalAddress()) {
        return address;
      }
    }
    // try to find one that is not link local
    for (final InetAddress address : allButLoopback) {
      if (!address.isLinkLocalAddress()) {
        return address;
      }
    }
    // all else fails, return localhost
    return InetAddress.getLocalHost();
  }
}
