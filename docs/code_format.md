


## Code Format


When discussing formatting, please keep in mind:
- https://en.wikipedia.org/wiki/Law_of_triviality
- http://bikeshed.com/
- The goal of formatting/conventions is to add healthy constraints to how code is written. This is to benefit people reading code allowing them to make valid assumptions.

### Format
Please import these IDE formatter templates:
- https://github.com/triplea-game/triplea/blob/master/triplea_java_eclipse_format_style.xml
- https://github.com/triplea-game/triplea/blob/master/triplea_java_eclipse_cleanup.xml


For the most part, following Google java style: http://google.github.io/styleguide/javaguide.html. Disagreements in formatting can be resolved by looking to the style guide.


### Line Endings
- use LF line endings for java files
- avoid using CRLF unless there is a pretty solid reason. For example, if it is known that many people are using notepad to open text files on a regular basis. In that case CRLF is nice for notepad. From a coding point of view we likely should try to present a UI to prevent users from having to manipulate text files. 

### Some Previous Format Discussions
- https://github.com/triplea-game/triplea/issues/18
- https://github.com/triplea-game/triplea/issues/19

### Current Deviations from google formatter
- setting id="org.eclipse.jdt.core.formatter.comment.line_length" value="120"
- setting id="org.eclipse.jdt.core.formatter.join_lines_in_comments" value="false"
- setting id="org.eclipse.jdt.core.formatter.join_wrapped_lines" value="false"
- setting id="org.eclipse.jdt.core.formatter.lineSplit" value="120"
- setting id="org.eclipse.jdt.core.formatter.put_empty_statement_on_new_line" value="true"


### Installing the formatter in IntelliJ
1. Import the xml into intellij's java code style, and then choose it as your style
2. Edit the general and java code style to use 2 spaces for tab size and indent, and 4 for continuation indent.
3. On java code style imports tab, change class and name counts to 99, and then delete the two lines under "packages to use import with *"
4. Download the plugin "Eclipse Code Formatter" and restart intellij
5. Open up settings again and go to 'other settings' and enable the plugin. Make sure the following are checked: "Use the Eclipse code formatter", "Enable Java", "Use Eclipse 4.4 Java formatter / otherwise Eclipse 4.5 ....", "Optimize Imports"
6. Browse and select the Eclipse java formatter config file.

### Installing the formatter in eclipse
Project preferences > Java > java format, import and select the eclipse format xml file. (note, be sure you repeat this if the formatter ever changes to pick up any updates)


