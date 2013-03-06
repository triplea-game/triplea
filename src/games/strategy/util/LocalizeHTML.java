package games.strategy.util;

import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.ui.UIContext;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author veqryn
 * 
 */
public class LocalizeHTML
{
	private static final String ASSET_IMAGE_FOLDER = "doc/images/";
	private static final String ASSET_IMAGE_NOT_FOUND = "notFound.png";
	/*
	 * You would think that there would be a single standardized REGEX for pulling html links out of <img> tags and <a> tags.  
	 * But there isn't, and the internet seems to give million different answers, none of which work perfectly.
	 * So here are the best one I could find.
	 * Regex's found at http://www.mkyong.com/
	 */

	/* Match an <a></a> tag.
		(							#	start of group #1
		 ?i							#		all checking are case insensive
		   )						#	end of group #1
		    <a       		      	#	start with "<a"
		      (						#		start of group #2
		       [^>]+				#			anything except (">"), at least one character
		            )				#		end of group #2
		             >				#			follow by ">"
		              (.*?)			#				match anything
		                   </a>		#					end with "</a>
	*/
	private static final String PATTERN_HTML_A_TAG = "(?i)<a([^>]+)>(.*?)</a>";
	
	/* Match the href attribute.
		\s*													#	can start with whitespace
		   (?i)												#		all checking are case insensive
		       href											#			follow by "href" word
		           \s*=\s*									#				allows spaces on either side of the equal sign,
		                  (									#					start of group #1
		                   "([^"]*")						#						allow string with double quotes enclosed - "string"
		                            |						#						..or
		                             '[^']*'				#						allow string with single quotes enclosed - 'string'
		                                    |				#						..or
		                                     ([^'">]+)		#						can't contains one single quotes, double quotes ">"
			                                          )		#					end of group #1
	*/
	private static final String PATTERN_HTML_A_HREF_TAG = "\\s*(?i)href\\s*=\\s*(\"([^\"]*\")|'[^']*'|([^'\">\\s]+))";
	
	/* Match the <img /> tag */
	private static final String PATTERN_HTML_IMG_TAG = "(?i)<img([^>]+)/>";
	
	/* Match the src attribute */
	private static final String PATTERN_HTML_IMG_SRC_TAG = "\\s*(?i)src\\s*=\\s*(\"([^\"]*\")|'[^']*'|([^'\">\\s]+))";
	
	public static List<String> getAllAhrefLinksFromHTML(final String htmlText)
	{
		final List<String> result = new ArrayList<String>();
		final Pattern patternTag = Pattern.compile(PATTERN_HTML_A_TAG);
		final Pattern patternLink = Pattern.compile(PATTERN_HTML_A_HREF_TAG);
		final Matcher matcherTag = patternTag.matcher(htmlText);
		Matcher matcherLink;
		while (matcherTag.find())
		{
			final String href = matcherTag.group(1); // a tag
			// final String linkText = matcherTag.group(2); // link text
			// System.out.println("Tag Contents: " + href);
			// System.out.println("Tag Text: " + linkText);
			matcherLink = patternLink.matcher(href);
			while (matcherLink.find())
			{
				final String link = matcherLink.group(1); // href link
				// System.out.println("Link: " + link);
				result.add(link);
			}
		}
		return result;
	}
	
	public static List<String> getAllImgSrcLinksFromHTML(final String htmlText)
	{
		final List<String> result = new ArrayList<String>();
		final Pattern patternTag = Pattern.compile(PATTERN_HTML_IMG_TAG);
		final Pattern patternLink = Pattern.compile(PATTERN_HTML_IMG_SRC_TAG);
		final Matcher matcherTag = patternTag.matcher(htmlText);
		Matcher matcherLink;
		while (matcherTag.find())
		{
			final String href = matcherTag.group(1); // img tag
			// System.out.println("Tag Contents: " + href);
			matcherLink = patternLink.matcher(href);
			while (matcherLink.find())
			{
				final String link = matcherLink.group(1); // src link
				// System.out.println("Link: " + link);
				result.add(link);
			}
		}
		return result;
	}
	
	/**
	 * This is only useful once we are IN a game. Before we go into the game, resource loader will either be null, or be the last game's resource loader.
	 * 
	 * @param htmlText
	 * @return
	 */
	public static String localizeImgLinksInHTML(final String htmlText)
	{
		return localizeImgLinksInHTML(htmlText, UIContext.getResourceLoader(), null);
	}
	
	public static String localizeImgLinksInHTML(final String htmlText, final ResourceLoader resourceLoader, final String mapNameDir)
	{
		if (htmlText == null || (resourceLoader == null && (mapNameDir == null || mapNameDir.trim().length() == 0))) // comment out when testing
			return htmlText;
		ResourceLoader ourResourceLoader = resourceLoader;
		String rVal = htmlText;
		final Pattern patternTag = Pattern.compile(PATTERN_HTML_IMG_TAG);
		final Pattern patternLink = Pattern.compile(PATTERN_HTML_IMG_SRC_TAG);
		final Matcher matcherTag = patternTag.matcher(htmlText);
		Matcher matcherLink;
		while (matcherTag.find())
		{
			final String href = matcherTag.group(1); // img tag
			if (href == null)
				continue;
			// System.out.println("Tag Contents: " + href);
			matcherLink = patternLink.matcher(href);
			while (matcherLink.find())
			{
				final String fullLink = matcherLink.group(1); // src link
				if (fullLink != null && fullLink.length() > 2)
				{
					if (ourResourceLoader == null)
					{
						ourResourceLoader = ResourceLoader.getMapResourceLoader(mapNameDir, false);
					}
					final String link = fullLink.substring(1, fullLink.length() - 1); // remove quotes
					// System.out.println("Link: " + link);
					final String imageFileName = link.substring(Math.max((link.lastIndexOf("/") + 1), 0)); // remove full parent path
					URL replacementURL = ourResourceLoader.getResource(ASSET_IMAGE_FOLDER + imageFileName); // replace when testing with: "REPLACEMENTPATH/" + imageFileName;
					if (replacementURL == null || replacementURL.toString().length() == 0)
					{
						System.out.println("Could not find: <map>/" + ASSET_IMAGE_FOLDER + imageFileName);
						replacementURL = ourResourceLoader.getResource(ASSET_IMAGE_FOLDER + ASSET_IMAGE_NOT_FOUND);
					}
					if (replacementURL == null || replacementURL.toString().length() == 0)
					{
						System.err.println("Could not find: " + ASSET_IMAGE_FOLDER + ASSET_IMAGE_NOT_FOUND);
						continue;
					}
					// System.out.println("Replacement URL: " + replacementURL);
					rVal = rVal.replaceAll(link, replacementURL.toString());
				}
			}
		}
		return rVal;
	}
	
	public static void main(final String[] args)
	{
		final String htmlText = "<br><img src=\"http://tripleamaps.sourceforge.net/images/tww/TechOverview0.png\" alt=\"blah, blah\"/>"
					+ "\n<br /><img src=\"\" alt=\"<em>formatted, alt text for a no source image</em>\"/>"
					+ "\n<br/><img src=\"http://www.someWebsite.com/some%20Folder/someOtherFolder Which May Contain Spaces/somePicture.gif\"/>"
					+ "\n<br><img src=\"http://www.someWebsite.com/some%20Folder/someOtherFolder Which May Contain Spaces/some Picture%20WithSpaces.JPEG\" alt=\"<em>formatted, alt text</em>\"/>"
					+ "\n<br><img src='http://www.someWebsite.com/some%20Folder/someOtherFolder Which May Contain Spaces/some Picture%20WithSpaces2.JPEG' alt='formatted, alt text for single quote img tag'/>"
					+ "\n<br/><IMG ALT=\"reverse order alt and src tags and capitalized\" SRC=\"http://www.someWebsite.com/some%20Folder/someOtherFolder Which May Contain Spaces/somePicture2.gif\"/>"
					+ "\n<br /><br><img src=\"http://tripleamaps.sourceforge.net/images/tww/TechOverview1.png\" alt=\"technically legal to have separate end tag on img\"></img>"
					+ "\n<br>And here is a normal link in plain text: http://tripleamaps.sourceforge.net/images/tww/TechOverview1.png"
					+ "\n<br />And <SPAN>some regular old html <b>text that</b> might <i>have other</i> formatting in it</SPAN> and stuff."
					+ "\n<br />And now for some normal links as links:"
					+ "\n<br /><a href=\"http://tripleamaps.sourceforge.net/images/tww/TechOverview2.png\">picture</a>"
					+ "\n<br /><a href=\"http://tripleamaps.sourceforge.net/images/tww/TechOverview3.png\"/>"
					+ "\n<br /><A HREF=\"http://tripleamaps.sourceforge.net/images/tww/TechOverview4.png\"><em><SPAN>formatted, alt text, with capitalization</SPAN></em></A>"
					+ "\n<br /><a href=\"\"><em>formatted, alt text for a no href link</em></a>"
					+ "\n<br /><a target='_blank' href='http://www.someWebsite.com/some%20Folder/someOtherFolder Which May Contain Spaces/some Picture%20WithSpaces3.JPEG'><b>reverse order with single quotes and formatted text</b></a>";
		System.out.println(htmlText);
		System.out.println("\n\n\n");
		final List<String> links1 = getAllAhrefLinksFromHTML(htmlText);
		System.out.println(links1.toString());
		System.out.println("\n\n\n");
		final List<String> links2 = getAllImgSrcLinksFromHTML(htmlText);
		System.out.println(links2.toString());
		System.out.println("\n\n\n");
		final String newHTMLstring = localizeImgLinksInHTML(htmlText);
		System.out.println("\n" + newHTMLstring);
	}
}
