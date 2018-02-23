package games.strategy.net;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.primitives.Bytes;

import games.strategy.debug.ClientLogger;

/**
 * Provides access to the MAC address of the local node.
 */
public final class MacFinder {
  private static final String HASHED_MAC_ADDRESS_SALT = "MH";

  private MacFinder() {}

  /**
   * Should result in something like this: $1$MH$345ntXD4G3AKpAeHZdaGe3.
   *
   * @throws IllegalArgumentException If the MAC address is not available.
   */
  public static String getHashedMacAddress() {
    final String mac = getMacAddress();
    if (mac == null) {
      throw new IllegalArgumentException("You have an invalid MAC address or TripleA can't find your mac address");
    }
    return getHashedMacAddress(mac);
  }

  /**
   * Gets the hash of the specified MAC address.
   *
   * @param macAddressBytes The MAC address bytes.
   *
   * @return The hash of the specified MAC address.
   *
   * @throws IllegalArgumentException If the length of {@code macAddressBytes} is not six bytes.
   */
  public static String getHashedMacAddress(final byte[] macAddressBytes) {
    checkNotNull(macAddressBytes);
    checkArgument(macAddressBytes.length == 6, "MAC address length must be six bytes");

    return getHashedMacAddress(convertMacBytesToString(macAddressBytes));
  }

  private static String getHashedMacAddress(final String macAddress) {
    return games.strategy.util.Md5Crypt.crypt(macAddress, HASHED_MAC_ADDRESS_SALT);
  }

  private static String getMacAddress() {
    // We must try different methods of obtaining the mac address because not all the methods work on each system, and
    // if we can't obtain
    // the mac, we can't login to the lobby
    // First, try to get the mac address of the local host network interface
    try {
      final InetAddress address = InetAddress.getLocalHost();
      final NetworkInterface localHostNetworkInterface = NetworkInterface.getByInetAddress(address);
      if (localHostNetworkInterface != null) {
        final byte[] rawMac = localHostNetworkInterface.getHardwareAddress();
        final String mac = convertMacBytesToString(rawMac);
        if (isMacValid(mac)) {
          return mac;
        }
      }
    } catch (final SocketException | UnknownHostException e) {
      ClientLogger.logError("Error while trying to get a valid MAC adress", e);
    }
    // Next, try to get the mac address of the first network interfaces that has an accessible mac address
    try {
      for (final NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
        final byte[] rawMac = ni.getHardwareAddress();
        final String mac = convertMacBytesToString(rawMac);
        if (isMacValid(mac)) {
          return mac;
        }
      }
    } catch (final SocketException e) {
      ClientLogger.logError("Error while trying to get a valid MAC adress", e);
    }
    // Next, try to get the mac address by calling the 'getmac' app that exists in Windows, Mac, and possibly others.
    /*
     * Physical Address Transport Name
     * =================== ==========================================================
     * 00-1F-C6-F9-EC-E8 \Device\Tcpip_{99F55DF7-8C43-464C-A8A9-FA3F847467CB}
     */
    try {
      final String results = executeCommandAndGetResults("getmac");
      final String mac = tryToParseMacFromOutput(results, Arrays.asList("-", ":", "."), false);
      if (isMacValid(mac)) {
        return mac;
      }
    } catch (final Exception e) {
      ClientLogger.logQuietly("Error while trying to get mac address", e);
    }
    // Next, try to get the mac address by calling the 'ipconfig -all' app that exists in Windows and possibly others.
    /*
     * ...
     * Physical Address. . . . . . . . . : 00-1C-D3-F8-DC-E8
     * ...
     */
    try {
      final String results = executeCommandAndGetResults("ipconfig -all");
      final String mac = tryToParseMacFromOutput(results, Arrays.asList("-", ":", "."), false);
      if (isMacValid(mac)) {
        return mac;
      }
    } catch (final Exception e) {
      ClientLogger.logQuietly("Error while trying to get mac address", e);
    }
    try {
      // ipconfig -all does not work on my computer, while ipconfig /all does not work on others computers
      final String results = executeCommandAndGetResults("ipconfig /all");
      final String mac = tryToParseMacFromOutput(results, Arrays.asList("-", ":", "."), false);
      if (isMacValid(mac)) {
        return mac;
      }
    } catch (final Exception e) {
      ClientLogger.logQuietly("Error while trying to get mac address", e);
    }
    // Next, try to get the mac address by calling the 'ifconfig -a' app that exists in Linux and possibly others. May
    // have 1 or 2 spaces
    // between Ethernet and HWaddr, and may be wireless instead of ethernet.
    /*
     * ...
     * eth0 Link encap:Ethernet HWaddr 00:08:C7:1B:8C:02
     * ...
     */
    try {
      final String results = executeCommandAndGetResults("ifconfig -a");
      // Allow the parser to try adding a zero to
      // the beginning
      final String mac = tryToParseMacFromOutput(results, Arrays.asList(":", "-", "."), true);
      if (isMacValid(mac)) {
        return mac;
      }
    } catch (final Exception e) {
      ClientLogger.logQuietly("Error while trying to get mac address", e);
    }
    // Next, try to get the mac address by calling the '/sbin/ifconfig -a' app that exists in Linux and possibly others.
    // May have 1 or 2
    // spaces between Ethernet and HWaddr, and may be wireless instead of ethernet.
    /*
     * ...
     * eth0 Link encap:Ethernet HWaddr 00:08:C7:1B:8C:02
     * ...
     */
    try {
      final String results = executeCommandAndGetResults("/sbin/ifconfig -a");
      // Allow the parser to try adding a zero to
      // the beginning
      final String mac = tryToParseMacFromOutput(results, Arrays.asList(":", "-", "."), true);
      if (isMacValid(mac)) {
        return mac;
      }
    } catch (final Exception e) {
      ClientLogger.logQuietly("Error while trying to get mac address", e);
    }
    // Next, try to get the mac address by calling the 'dmesg' app that exists in FreeBSD and possibly others.
    /*
     * ...
     * [ 405.681688] wlan0_rename: associate with AP 00:16:f8:40:3e:bd
     * [ 405.683255] wlan0_rename: RX ReassocResp from 00:16:f8:40:3e:bd (capab=0x411 status=0 aid=4)
     * ...
     */
    try {
      final String results = executeCommandAndGetResults("dmesg");
      final String mac = tryToParseMacFromOutput(results, Arrays.asList(":", "-", "."), false);
      if (isMacValid(mac)) {
        return mac;
      }
    } catch (final Exception e) {
      ClientLogger.logQuietly("Error while trying to get mac address", e);
    }
    return null;
  }

  private static String executeCommandAndGetResults(final String command) {
    Process p = null;
    try {
      p = new ProcessBuilder(command).start();
    } catch (final Exception e) {
      try {
        p = Runtime.getRuntime().exec(command);
      } catch (final IOException e2) {
        ClientLogger.logQuietly("Ignoring error while executing command: " + command, e);
      }
    }
    if (p == null) {
      return null;
    }
    try {
      return IOUtils.toString(p.getInputStream(), Charset.defaultCharset());
    } catch (final IOException e) {
      ClientLogger.logQuietly("IOException while executing command: " + command, e);
      return null;
    }
  }

  private static String convertMacBytesToString(final byte[] mac) {
    if (mac == null) {
      return null;
    }
    return Joiner.on('.')
        .join(FluentIterable.from(Bytes.asList(mac)).transform(macbyte -> String.format("%02X", macbyte)));
  }

  private static boolean isMacValid(final String mac) {
    if ((mac == null) || (mac.length() != 17) || !mac.contains(".") || !mac.matches("[0-9A-Fa-f.]+")) {
      return false;
    }
    final char[] chars = mac.toCharArray();
    int periodCount = 0;
    int nonZeroNumberCount = 0;
    int i = 1;
    for (final char ch : chars) {
      if ((ch == '.') && ((i % 3) != 0)) {
        return false;
      }
      if (ch == '.') {
        periodCount++;
      }
      if ((ch != '.') && (ch != '0')) {
        nonZeroNumberCount++;
      }
      i++;
    }
    return !((periodCount != 5) || mac.equals("00.00.00.00.00.E0") || (nonZeroNumberCount == 0));
  }

  private static String tryToParseMacFromOutput(final String output, final List<String> possibleSeparators,
      final boolean allowAppendedZeroCheck) {
    if ((output == null) || (output.trim().length() < 6)) {
      return null;
    }
    for (final String separator : possibleSeparators) {
      String leftToSearch = output;
      while ((leftToSearch != null) && (leftToSearch.length() > 0) && leftToSearch.contains(separator)) {
        int macStartIndex = Math.max(0, leftToSearch.indexOf(separator) - 2);
        String rawMac = leftToSearch.substring(macStartIndex, Math.min(macStartIndex + 17, leftToSearch.length()));
        if ((rawMac != null) && (rawMac.length() > 0)) {
          String mac = rawMac.replace(separator, ".");
          if (isMacValid(mac)) {
            return mac;
          } else if (allowAppendedZeroCheck && rawMac.substring(2, 3).equals(separator)) {
            // If mac is invalid, see if it works after adding a zero to the front
            macStartIndex = Math.max(0, leftToSearch.indexOf(separator) - 1);
            rawMac = "0" + leftToSearch.substring(macStartIndex, Math.min(macStartIndex + 16, leftToSearch.length()));
            mac = rawMac.replace(separator, ".");
            if (isMacValid(mac)) {
              return mac;
            }
          }
        }
        // We only invalidate the one separator char and what's before it, so that '-ether 89-94-19...' would not fail,
        // then cause the -
        // after 89 to get ignored (Not sure if this situation really occurs)
        leftToSearch = leftToSearch.substring(Math.min(macStartIndex + 1, leftToSearch.length()));
      }
    }
    return null;
  }

  /**
   * Indicates the specified value is a valid hashed MAC address.
   *
   * @param value The value to test.
   *
   * @return {@code true} if the specified value is a valid hashed MAC address; otherwise {@code false}.
   */
  public static boolean isValidHashedMacAddress(final String value) {
    checkNotNull(value);

    try {
      return HASHED_MAC_ADDRESS_SALT.equals(games.strategy.util.Md5Crypt.getSalt(value));
    } catch (final IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Removes the prefix from the specified hashed MAC address.
   *
   * @param hashedMacAddress The hashed MAC address whose prefix is to be removed.
   *
   * @return The hashed MAC address without its prefix.
   *
   * @throws IllegalArgumentException If {@code hashedMacAddress} is not a valid hashed MAC address.
   */
  public static String trimHashedMacAddressPrefix(final String hashedMacAddress) {
    checkNotNull(hashedMacAddress);

    return games.strategy.util.Md5Crypt.getHash(hashedMacAddress);
  }
}
