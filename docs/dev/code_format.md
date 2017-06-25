---
layout: longpage
title: Code Format
permalink: /dev_docs/dev/code_format/
---

For the most part, we are following [Google java style](http://google.github.io/styleguide/javaguide.html)

## Format
Please import these IDE formatter templates:

- [https://github.com/triplea-game/triplea/blob/master/eclipse/format/triplea_java_eclipse_format_style.xml](https://github.com/triplea-game/triplea/blob/master/eclipse/format/triplea_java_eclipse_format_style.xml)
- [https://github.com/triplea-game/triplea/blob/master/eclipse/format/triplea_java_eclipse_cleanup.xml](https://github.com/triplea-game/triplea/blob/master/eclipse/format/triplea_java_eclipse_cleanup.xml)
- [https://github.com/triplea-game/triplea/blob/master/eclipse/format/triplea.importorder](https://github.com/triplea-game/triplea/blob/master/eclipse/format/triplea.importorder)

### Installing the formatter in IntelliJ
1. Import the xml into intellij's java code style, and then choose it as your style
2. Edit the general and java code style to use 2 spaces for tab size and indent, and 4 for continuation indent.
3. On java code style imports tab, change class and name counts to 99, and then delete the two lines under "packages to use import with \*"
4. Download the plugin "Eclipse Code Formatter" and restart intellij
5. Open up settings again and go to 'other settings' and enable the plugin. Make sure the following are checked: "Use the Eclipse code formatter", "Enable Java", "Use Eclipse 4.4 Java formatter / otherwise Eclipse 4.5 ....", "Optimize Imports"
6. Browse and select the Eclipse java formatter config file.

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
