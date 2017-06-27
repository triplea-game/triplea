## IDE Code Format Setup

### Eclipse

Project preferences > Java > java format, import and select the eclipse format xml file.

- [https://github.com/triplea-game/triplea/blob/master/eclipse/format/triplea_java_eclipse_format_style.xml](https://github.com/triplea-game/triplea/blob/master/eclipse/format/triplea_java_eclipse_format_style.xml)
- [https://github.com/triplea-game/triplea/blob/master/eclipse/format/triplea_java_eclipse_cleanup.xml](https://github.com/triplea-game/triplea/blob/master/eclipse/format/triplea_java_eclipse_cleanup.xml)
- [https://github.com/triplea-game/triplea/blob/master/eclipse/format/triplea.importorder](https://github.com/triplea-game/triplea/blob/master/eclipse/format/triplea.importorder)

### IntelliJ
- Use plugin "Eclipse Code Formatter"


## Additional Notes

### Line Endings
- use LF line endings for java files, avoid using CRLF unless there is a pretty solid reason for it.

### Deviations from google formatter:
```
setting id="org.eclipse.jdt.core.formatter.comment.line_length" value="120"
setting id="org.eclipse.jdt.core.formatter.join_lines_in_comments" value="false"
setting id="org.eclipse.jdt.core.formatter.join_wrapped_lines" value="false"
setting id="org.eclipse.jdt.core.formatter.lineSplit" value="120"
setting id="org.eclipse.jdt.core.formatter.put_empty_statement_on_new_line" value="true"
```
