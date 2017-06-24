
For the most part, we are following [Google java style](http://google.github.io/styleguide/javaguide.html)

## Format
Please import these IDE formatter templates:

- [https://github.com/triplea-game/triplea/blob/master/eclipse/format/triplea_java_eclipse_format_style.xml](https://github.com/triplea-game/triplea/blob/master/eclipse/format/triplea_java_eclipse_format_style.xml)
- [https://github.com/triplea-game/triplea/blob/master/eclipse/format/triplea_java_eclipse_cleanup.xml](https://github.com/triplea-game/triplea/blob/master/eclipse/format/triplea_java_eclipse_cleanup.xml)
- [https://github.com/triplea-game/triplea/blob/master/eclipse/format/triplea.importorder](https://github.com/triplea-game/triplea/blob/master/eclipse/format/triplea.importorder)

### Installing the formatter in IntelliJ
- Download the plugin "Eclipse Code Formatter", use that to import the eclipse formatter.  Do not simply use the XML file without the plugin.
- Open up settings and go to 'other settings' and enable the plugin. Make sure the following are checked: 
   - "Use the Eclipse code formatter", 
   - "Enable Java", 
   - "Use Eclipse 4.4 Java formatter / otherwise Eclipse 4.5 ....", 
   - "Optimize Imports"

### Installing the formatter in eclipse
Project preferences > Java > java format, import and select the eclipse format xml file.

## Current Deviations from google formatter
- setting id="org.eclipse.jdt.core.formatter.comment.line_length" value="120"
- setting id="org.eclipse.jdt.core.formatter.join_lines_in_comments" value="false"
- setting id="org.eclipse.jdt.core.formatter.join_wrapped_lines" value="false"
- setting id="org.eclipse.jdt.core.formatter.lineSplit" value="120"
- setting id="org.eclipse.jdt.core.formatter.put_empty_statement_on_new_line" value="true"

## Line Endings
- use LF line endings for java files, avoid using CRLF unless there is a pretty solid reason for it.
