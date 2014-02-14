package games.strategy.engine.lobby.server.login;

import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;
import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.userDB.BadWordController;
import games.strategy.engine.lobby.server.userDB.BannedIpController;
import games.strategy.engine.lobby.server.userDB.BannedMacController;
import games.strategy.engine.lobby.server.userDB.BannedUsernameController;
import games.strategy.engine.lobby.server.userDB.DBUserController;
import games.strategy.net.ILoginValidator;
import games.strategy.util.MD5Crypt;
import games.strategy.util.Tuple;
import games.strategy.util.Version;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class LobbyLoginValidator implements ILoginValidator
{
	static final String THATS_NOT_A_NICE_NAME = "That's not a nice name.";
	static final String YOU_HAVE_BEEN_BANNED = "You have been banned from the TripleA lobby.";
	static final String USERNAME_HAS_BEEN_BANNED = "This username is banned, please create a new one.";
	static final String UNABLE_TO_OBTAIN_MAC = "Unable to obtain mac address.";
	static final String INVALID_MAC = "Invalid mac address.";
	private final static Logger s_logger = Logger.getLogger(LobbyLoginValidator.class.getName());
	public static final String LOBBY_VERSION = "LOBBY_VERSION";
	public static final String REGISTER_NEW_USER_KEY = "REGISTER_USER";
	public static final String ANONYMOUS_LOGIN = "ANONYMOUS_LOGIN";
	public static final String LOBBY_WATCHER_LOGIN = "LOBBY_WATCHER_LOGIN";
	public static final String LOGIN_KEY = "LOGIN";
	public static final String HASHED_PASSWORD_KEY = "HASHEDPWD";
	public static final String EMAIL_KEY = "EMAIL";
	public static final String SALT_KEY = "SALT";
	
	public LobbyLoginValidator()
	{
	}
	
	public Map<String, String> getChallengeProperties(final String userName, final SocketAddress remoteAddress)
	{
		// we need to give the user the salt key for the username
		final String password = new DBUserController().getPassword(userName);
		final Map<String, String> rVal = new HashMap<String, String>();
		if (password != null)
			rVal.put(SALT_KEY, MD5Crypt.getSalt(MD5Crypt.MAGIC, password));
		return rVal;
	}
	
	public String verifyConnection(final Map<String, String> propertiesSentToClient, final Map<String, String> propertiesReadFromClient, final String clientName, final String clientMac,
				final SocketAddress remoteAddress)
	{
		final String error = verifyConnectionInternal(propertiesSentToClient, propertiesReadFromClient, clientName, clientMac, remoteAddress);
		if (error != null)
		{
			s_logger.info("Bad login attemp from " + remoteAddress + " for user " + clientName + " error:" + error);
			AccessLog.failedLogin(clientName, ((InetSocketAddress) remoteAddress).getAddress(), error);
		}
		else
		{
			s_logger.info("Successful login from:" + remoteAddress + " for user:" + clientName);
			AccessLog.successfulLogin(clientName, ((InetSocketAddress) remoteAddress).getAddress());
		}
		return error;
	}
	
	private String verifyConnectionInternal(final Map<String, String> propertiesSentToClient, final Map<String, String> propertiesReadFromClient, final String clientName, final String hashedMac,
				final SocketAddress remoteAddress)
	{
		if (propertiesReadFromClient == null)
			return "No Client Properties";
		final String clientVersionString = propertiesReadFromClient.get(LOBBY_VERSION);
		if (clientVersionString == null)
			return "No Client Version";
		final Version clientVersion = new Version(clientVersionString);
		if (!clientVersion.equals(LobbyServer.LOBBY_VERSION))
		{
			return "Wrong version, we require" + LobbyServer.LOBBY_VERSION.toString() + " but trying to log in with " + clientVersionString;
		}
		for (final String s : getBadWords())
		{
			if (clientName.toLowerCase().contains(s.toLowerCase()))
			{
				return THATS_NOT_A_NICE_NAME;
			}
		}
		final String remoteIp = ((InetSocketAddress) remoteAddress).getAddress().getHostAddress();
		final Tuple<Boolean, Timestamp> ipBanned = new BannedIpController().isIpBanned(remoteIp);
		if (ipBanned.getFirst())
		{
			return YOU_HAVE_BEEN_BANNED + " " + getBanDurationBreakdown(ipBanned.getSecond());
		}
		if (hashedMac == null)
		{
			return UNABLE_TO_OBTAIN_MAC;
		}
		if (hashedMac.length() != 28 || !hashedMac.startsWith(MD5Crypt.MAGIC + "MH$") || !hashedMac.matches("[0-9a-zA-Z$./]+"))
		{
			return INVALID_MAC; // Must have been tampered with
		}
		final Tuple<Boolean, Timestamp> macBanned = new BannedMacController().isMacBanned(hashedMac);
		if (macBanned.getFirst())
		{
			return YOU_HAVE_BEEN_BANNED + " " + getBanDurationBreakdown(macBanned.getSecond());
		}
		// test for username ban after testing normal bans, because if it is only a username ban then the user should know they can change their name
		final Tuple<Boolean, Timestamp> usernameBanned = new BannedUsernameController().isUsernameBanned(clientName);
		if (usernameBanned.getFirst())
		{
			return USERNAME_HAS_BEEN_BANNED + " " + getBanDurationBreakdown(usernameBanned.getSecond());
		}
		if (propertiesReadFromClient.containsKey(REGISTER_NEW_USER_KEY))
		{
			return createUser(propertiesReadFromClient, clientName);
		}
		if (propertiesReadFromClient.containsKey(ANONYMOUS_LOGIN))
		{
			return anonymousLogin(propertiesReadFromClient, clientName);
		}
		else
		{
			return validatePassword(propertiesSentToClient, propertiesReadFromClient, clientName);
		}
	}
	
	static String getBanDurationBreakdown(final Timestamp stamp)
	{
		if (stamp == null)
			return "Banned Forever";
		final long millis = stamp.getTime() - System.currentTimeMillis();
		if (millis < 0)
			return "Ban time left: 1 Minute";
		long seconds = Math.max(1, TimeUnit.MILLISECONDS.toSeconds(millis));
		final int minutesInSeconds = 60;
		final int hoursInSeconds = 60 * 60;
		final int daysInSeconds = 60 * 60 * 24;
		final long days = seconds / daysInSeconds;
		seconds -= days * daysInSeconds;
		final long hours = seconds / hoursInSeconds;
		seconds -= hours * hoursInSeconds;
		final long minutes = Math.max(1, seconds / minutesInSeconds);
		/*
		final long days = TimeUnit.MILLISECONDS.toDays(millis);
		millis -= TimeUnit.DAYS.toMillis(days);
		final long hours = TimeUnit.MILLISECONDS.toHours(millis);
		millis -= TimeUnit.HOURS.toMillis(hours);
		final long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) + 1;*/
		final StringBuilder sb = new StringBuilder(64);
		sb.append("Ban time left: ");
		if (days > 0)
		{
			sb.append(days);
			sb.append(" Days ");
		}
		if (hours > 0)
		{
			sb.append(hours);
			sb.append(" Hours ");
		}
		if (minutes > 0)
		{
			sb.append(minutes);
			sb.append(" Minutes ");
		}
		return (sb.toString());
	}
	
	private List<String> getBadWords()
	{
		return new BadWordController().list();
	}
	
	private String validatePassword(final Map<String, String> propertiesSentToClient, final Map<String, String> propertiesReadFromClient, final String clientName)
	{
		final DBUserController userController = new DBUserController();
		if (!userController.login(clientName, propertiesReadFromClient.get(HASHED_PASSWORD_KEY)))
		{
			if (userController.doesUserExist(clientName))
			{
				return "Incorrect password";
			}
			else
			{
				return "Username does not exist";
			}
		}
		else
		{
			return null;
		}
	}
	
	private String anonymousLogin(final Map<String, String> propertiesReadFromClient, final String userName)
	{
		if (new DBUserController().doesUserExist(userName))
			return "Can't login anonymously, username already exists";
		if (propertiesReadFromClient.get(LOBBY_WATCHER_LOGIN) != null && propertiesReadFromClient.get(LOBBY_WATCHER_LOGIN).equals(Boolean.TRUE.toString())) // If this is a lobby watcher, use a different set of validation
		{
			if (!userName.endsWith(InGameLobbyWatcher.LOBBY_WATCHER_NAME))
				return "Lobby watcher usernames must end with 'lobby_watcher'";
			final String hostName = userName.substring(0, userName.indexOf(InGameLobbyWatcher.LOBBY_WATCHER_NAME));
			final String issue = DBUserController.validateUserName(hostName);
			if (issue != null)
				return issue;
		}
		else
		{
			final String issue = DBUserController.validateUserName(userName);
			if (issue != null)
				return issue;
		}
		return null;
	}
	
	private String createUser(final Map<String, String> propertiesReadFromClient, final String userName)
	{
		final String email = propertiesReadFromClient.get(EMAIL_KEY);
		final String hashedPassword = propertiesReadFromClient.get(HASHED_PASSWORD_KEY);
		final DBUserController controller = new DBUserController();
		final String error = controller.validate(userName, email, hashedPassword);
		if (error != null)
		{
			return error;
		}
		try
		{
			controller.createUser(userName, email, hashedPassword, false);
			return null;
		} catch (final IllegalStateException ise)
		{
			return ise.getMessage();
		}
	}
	/*
	public static void main(final String[] args)
	{
		final Timestamp t = new Timestamp(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 2);
		System.out.println(getBanDurationBreakdown(t));
	}*/
}
