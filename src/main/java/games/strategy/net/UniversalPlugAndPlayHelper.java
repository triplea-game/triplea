package games.strategy.net;

import java.awt.Component;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.sbbi.upnp.impls.InternetGatewayDevice;
import net.sbbi.upnp.messages.UPNPResponseException;

public class UniversalPlugAndPlayHelper {
  private int port = 3300;
  private InetAddress local = null;
  private InternetGatewayDevice m_device = null;

  public UniversalPlugAndPlayHelper(final int port) {
    this.port = port;
  }

  public static boolean attemptAddingPortForwarding(final Component parent, final int port) {
    final UniversalPlugAndPlayHelper upnpHelper = new UniversalPlugAndPlayHelper(port);
    final JTextArea textArea = new JTextArea();
    textArea.setEditable(false);
    final String error = upnpHelper.attemptAddingPortForwarding(textArea);
    textArea.append("\r\n \r\n \r\n \r\n");
    final boolean worked = (error == null);
    final String textResult;
    if (worked) {
      textResult = "It looks like it worked.  This program will close now.\r\n"
          + "Please try hosting again, to see if it 'really' worked.\r\n"
          + "Remember that your firewall must allow TripleA, or else you still won't be able to host.\r\n";
    } else {
      textResult = "It appears TripleA failed to set your Port Forwarding.\r\n"
          + "Please make sure UPnP is turned on, in your router's settings.\r\n\r\n"
          + "If you still cannot get TripleA to set them correctly, then you must set them yourself!\r\n"
          + "See 'How To Host...' in the help menu, at the top of the lobby screen in order to manually set "
          + "them.\r\n\r\n"
          + "\r\nThis program will close now.\r\n";
    }
    System.out.println(textResult);
    textArea.append(textResult);
    JOptionPane.showMessageDialog(parent, new JScrollPane(textArea), "Setting Port Forwarding with UPnP",
        JOptionPane.INFORMATION_MESSAGE);
    return worked;
  }

  private String attemptAddingPortForwarding(final JTextArea textArea) {
    System.out.println("Starting Universal Plug and Play (UPnP) add port forward map script.");
    textArea.append("Starting Universal Plug and Play (UPnP) add port forward map script.\r\n");
    final String localError = findLocalInetAddress();
    if (localError != null) {
      textArea.append(localError + "\r\n");
      return localError;
    }
    textArea.append("Found Local IP/Inet Address to use: " + local + "\r\n");
    final String gatewayError = findInternetGatewayDevice();
    if (gatewayError != null) {
      textArea.append(gatewayError + "\r\n");
      return gatewayError;
    }
    textArea.append("Internet Gateway Device (normally a router) found!\r\n");
    final String addPortError = addPortForwardUPNP();
    if (addPortError != null) {
      textArea.append(addPortError + "\r\n");
      return addPortError;
    }
    textArea.append("Port Forwarding map added successfully.\r\n");
    return null;
  }

  private String addPortForwardUPNP() {
    final int internalPort = port;
    final int externalPort = port;
    // System.out.println("Attempting to map port on " + m_device.msgFactory.service.serviceType + " service");
    System.out.print("Adding mapping from ");
    try {
      System.out.print(m_device.getExternalIPAddress());
    } catch (final UPNPResponseException | IOException e1) {
      // ignore
    }
    System.out.println(":" + externalPort);
    System.out.println("To " + local.getHostAddress() + ":" + internalPort);
    boolean mapped = false;
    try {
      mapped = m_device.addPortMapping("TripleA Game Hosting", null, internalPort, externalPort, local.getHostAddress(), 0, "TCP");
    } catch (final IOException e) {
      System.out.println("Port Mapping Failed! Please try to Forward Ports manually! \r\n " + e.getMessage());
      return "Port Mapping Failed! Please try to Forward Ports manually! \r\n " + e.getMessage();
    } catch (final UPNPResponseException e) {
      System.out.println("Port Mapping Failed! Please try to Forward Ports manually! \r\n " + e.getMessage());
      return "Port Mapping Failed! Please try to Forward Ports manually! \r\n " + e.getMessage();
    }
    if (!mapped) {
      System.out.println("Port Mapping Failed! Please try to Forward Ports manually!");
      return "Port Mapping Failed! Please try to Forward Ports manually!";
    }
    System.out.println("Success. Port Forwarding map added.");
    return null;
  }

  private String findInternetGatewayDevice() {
    System.out.println("Attempting to find internet gateway device (normally a router).");
    InternetGatewayDevice[] devices = null;
    try {
      devices = InternetGatewayDevice.getDevices(2000);
    } catch (final IOException e) {
      System.out.println("Router/Device UPnP turned off. Or no Routers/Devices found.  "
          + "Please make sure your router's UPNP is turned on! \r\n "
          + e.getMessage());
      return "Router/Device UPnP turned off. Or no Routers/Devices found.  "
          + "Please make sure your router's UPNP is turned on! \r\n "
          + e.getMessage();
    }
    if (devices == null || 1 > devices.length) {
      System.out.println("Router/Device UPnP turned off. Or no Routers/Devices found.  "
          + "Please make sure your router's UPNP is turned on!");
      return "Router/Device UPnP turned off. Or no Routers/Devices found.  "
          + "Please make sure your router's UPNP is turned on!";
    }
    m_device = devices[0];
    System.out.println("Device found!");
    return null;
  }

  private String findLocalInetAddress() {
    local = null;
    System.out.println("Attempting to find local ip/inet address.");
    try {
      final Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
      while (ifaces.hasMoreElements() && local == null) {
        final NetworkInterface iface = ifaces.nextElement();
        final Enumeration<InetAddress> addresses = iface.getInetAddresses();
        while (addresses.hasMoreElements() && local == null) {
          final InetAddress addr = addresses.nextElement();
          if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
            local = addr;
          }
        }
      }
    } catch (final SocketException e) {
      local = null;
      System.out.println("Could not determine local ip/inet address!");
      return "Could not determine local ip/inet address! \r\n " + e.getMessage();
    }
    if (local == null) {
      System.out.println("Could not determine local ip/inet address!");
      return "Could not determine local ip/inet address!";
    }
    System.out.println("Local Address to use: " + local);
    return null;
  }
}
