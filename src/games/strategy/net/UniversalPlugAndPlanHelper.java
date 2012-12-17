package games.strategy.net;

import java.awt.Component;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Enumeration;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.sbbi.upnp.impls.InternetGatewayDevice;
import net.sbbi.upnp.messages.UPNPResponseException;

/**
 * 
 * @author veqryn
 * 
 */
public class UniversalPlugAndPlanHelper
{
	private int m_port = 3300;
	private InetAddress m_local = null;
	private InternetGatewayDevice m_device = null;
	private static UniversalPlugAndPlanHelper s_lastInstance = null;
	
	public static void main(final String[] args)
	{
		UniversalPlugAndPlanHelper.attemptAddingPortForwarding(null, 3300);
	}
	
	public UniversalPlugAndPlanHelper(final int port)
	{
		m_port = port;
	}
	
	public static UniversalPlugAndPlanHelper getLastInstance()
	{
		return s_lastInstance;
	}
	
	public static boolean attemptAddingPortForwarding(final Component parent, final int port)
	{
		final UniversalPlugAndPlanHelper upnpHelper = new UniversalPlugAndPlanHelper(port);
		final JTextArea textArea = new JTextArea();
		textArea.setEditable(false);
		final String error = upnpHelper.attemptAddingPortForwarding(textArea);
		textArea.append("\r\n \r\n \r\n \r\n");
		final boolean worked = (error == null);
		final String textResult;
		if (worked)
		{
			textResult = "It looks like it worked.  This program will close now.\r\n"
						+ "Please try hosting again, to see if it 'really' worked.\r\n"
						+ "Remember that your firewall must allow TripleA, or else you still won't be able to host.\r\n";
		}
		else
		{
			textResult = "It appears TripleA failed to set your Port Forwarding.\r\n"
						+ "Please make sure UPnP is turned on, in your router's settings.\r\n\r\n"
						+ "If you still can not get TripleA to set them correctly, then you must set them yourself!\r\n"
						+ "See 'How To Host...' in the help menu, at the top of the lobby screen in order to manually set them.\r\n\r\n"
						+ "\r\nThis program will close now.\r\n";
		}
		System.out.println(textResult);
		textArea.append(textResult);
		JOptionPane.showMessageDialog(parent, new JScrollPane(textArea), "Setting Port Forwarding with UPnP", JOptionPane.INFORMATION_MESSAGE);
		return worked;
	}
	
	public String attemptAddingPortForwarding(final JTextArea textArea)
	{
		System.out.println("Starting Universal Plug and Play (UPnP) add port forward map script.");
		textArea.append("Starting Universal Plug and Play (UPnP) add port forward map script.\r\n");
		final String localError = findLocalInetAddress();
		if (localError != null)
		{
			textArea.append(localError + "\r\n");
			return localError;
		}
		textArea.append("Found Local IP/Inet Address to use: " + m_local + "\r\n");
		final String gatewayError = findInternetGatewayDevice();
		if (gatewayError != null)
		{
			textArea.append(gatewayError + "\r\n");
			return gatewayError;
		}
		textArea.append("Internet Gateway Device (normally a router) found!\r\n");
		final String addPortError = addPortForwardUPNP();
		if (addPortError != null)
		{
			textArea.append(addPortError + "\r\n");
			return addPortError;
		}
		textArea.append("Port Forwarding map added successfully.\r\n");
		/*
		final String testError = testConnection();
		if (testError != null)
		{
			textArea.append(testError);
			return testError;
		}
		textArea.append("Test Connection made!");
		*/
		s_lastInstance = this;
		return null;
	}
	
	public String testConnection()
	{
		System.out.println("Waiting for a connection");
		final int internalPort = m_port;
		boolean connection = false;
		// boolean bytes = false;
		ServerSocket ss = null;
		try
		{
			ss = new ServerSocket(internalPort);
			ss.setSoTimeout(5000);
			try
			{
				final Socket s = ss.accept();
				connection = true;
				final InputStream in = s.getInputStream();
				while (in.available() > 0)
				{
					System.out.println("byte : " + in.read());
					// bytes = true;
				}
			} catch (final SocketTimeoutException stoe)
			{
				System.out.println("Connection Test Timed Out. Port Forward may still work anyway.");
				return "Connection Test Timed Out. Port Forward may still work anyway.";
			} finally
			{
				ss.close();
			}
		} catch (final IOException e)
		{
			System.out.println("Connection Test Timed Out. Port Forward may still work anyway. \r\n " + e.getMessage());
			return "Connection Test Timed Out. Port Forward may still work anyway. \r\n " + e.getMessage();
		} finally
		{
			if (ss != null)
				try
				{
					ss.close();
				} catch (final IOException e)
				{
					e.printStackTrace();
				}
		}
		if (!connection)
		{
			System.out.println("Connection Test Timed Out. Port Forward may still work anyway.");
			return "Connection Test Timed Out. Port Forward may still work anyway.";
		}
		System.out.println("Connection made!");
		return null;
	}
	
	public String removePortForwardUPNP()
	{
		System.out.println("Attempting to remove Port Forwarding");
		final int externalPort = m_port;
		boolean removed = false;
		try
		{
			removed = m_device.deletePortMapping(null, externalPort, "TCP");
		} catch (final IOException e)
		{
			System.out.println("Failed to remove port mapping! \r\n " + e.getMessage());
			return "Failed to remove port mapping! \r\n " + e.getMessage();
		} catch (final UPNPResponseException e)
		{
			System.out.println("Failed to remove port mapping! \r\n " + e.getMessage());
			return "Failed to remove port mapping! \r\n " + e.getMessage();
		}
		if (!removed)
		{
			System.out.println("Failed to remove port mapping!");
			return "Failed to remove port mapping!";
		}
		System.out.println("Success. Port Forwarding map removed.");
		return null;
	}
	
	private String addPortForwardUPNP()
	{
		final int internalPort = m_port;
		final int externalPort = m_port;
		// System.out.println("Attempting to map port on " + m_device.msgFactory.service.serviceType + " service");
		System.out.print("Adding mapping from ");
		try
		{
			System.out.print(m_device.getExternalIPAddress());
		} catch (final UPNPResponseException e1)
		{
		} catch (final IOException e1)
		{
		}
		System.out.println(":" + externalPort);
		System.out.println("To " + m_local.getHostAddress() + ":" + internalPort);
		boolean mapped = false;
		try
		{
			mapped = m_device.addPortMapping("TripleA Game Hosting", "TCP", null, externalPort, m_local.getHostAddress(), internalPort, 0);
		} catch (final IOException e)
		{
			System.out.println("Port Mapping Failed! Please try to Forward Ports manually! \r\n " + e.getMessage());
			return "Port Mapping Failed! Please try to Forward Ports manually! \r\n " + e.getMessage();
		} catch (final UPNPResponseException e)
		{
			System.out.println("Port Mapping Failed! Please try to Forward Ports manually! \r\n " + e.getMessage());
			return "Port Mapping Failed! Please try to Forward Ports manually! \r\n " + e.getMessage();
		}
		if (!mapped)
		{
			System.out.println("Port Mapping Failed! Please try to Forward Ports manually!");
			return "Port Mapping Failed! Please try to Forward Ports manually!";
		}
		System.out.println("Success. Port Forwarding map added.");
		return null;
	}
	
	private String findInternetGatewayDevice()
	{
		System.out.println("Attempting to find internet gateway device (normally a router).");
		InternetGatewayDevice[] devices = null;
		try
		{
			devices = InternetGatewayDevice.getDevices(2000);
		} catch (final IOException e)
		{
			System.out.println("Router/Device UPnP turned off. Or no Routers/Devices found.  Please make sure your router's UPNP is turned on! \r\n " + e.getMessage());
			return "Router/Device UPnP turned off. Or no Routers/Devices found.  Please make sure your router's UPNP is turned on! \r\n " + e.getMessage();
		}
		if (devices == null || 1 > devices.length)
		{
			System.out.println("Router/Device UPnP turned off. Or no Routers/Devices found.  Please make sure your router's UPNP is turned on!");
			return "Router/Device UPnP turned off. Or no Routers/Devices found.  Please make sure your router's UPNP is turned on!";
		}
		m_device = devices[0];
		System.out.println("Device found!");
		return null;
	}
	
	private String findLocalInetAddress()
	{
		m_local = null;
		System.out.println("Attempting to find local ip/inet address.");
		try
		{
			final Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
			while (ifaces.hasMoreElements() && m_local == null)
			{
				final NetworkInterface iface = ifaces.nextElement();
				final Enumeration<InetAddress> addresses = iface.getInetAddresses();
				
				while (addresses.hasMoreElements() && m_local == null)
				{
					final InetAddress addr = addresses.nextElement();
					if (addr instanceof Inet4Address && !addr.isLoopbackAddress())
					{
						m_local = addr;
					}
				}
			}
		} catch (final SocketException e)
		{
			m_local = null;
			System.out.println("Could not determine local ip/inet address!");
			return "Could not determine local ip/inet address! \r\n " + e.getMessage();
		}
		if (m_local == null)
		{
			System.out.println("Could not determine local ip/inet address!");
			return "Could not determine local ip/inet address!";
		}
		System.out.println("Local Address to use: " + m_local);
		return null;
	}
}
