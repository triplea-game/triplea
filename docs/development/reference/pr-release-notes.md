# PR Release Notes

Include a release note for notable changes that players would probably want to know about.  Do not include a release note if there is no visible change to players or if the change is so overly technical players would not care.

The target audience for release notes are players, they should be very human readable and easy to understand. To word a release note well, pretend a player in lobby asked you "what changed in this update from the last version?".

## Syntax

The release note comment is a tuple that is placed between special RELEASE_NOTE and END_RELEASE_NOTE comments. The first part of the release note comment is the update type, followed by a pipe and then the release note comment.

```
<!--RELEASE_NOTE-->[UPDATE TYPE]|[RELEASE NOTE COMMENT]<!--END_RELEASE_NOTE-->
```

**UPDATE_TYPE** should be one of:  { FIX, CHANGE, NEW }

### Example formats:

```
<!--RELEASE_NOTE-->FIX|Game can crash when attacking neutrals<!--END_RELEASE_NOTE-->
<!--RELEASE_NOTE-->CHANGE|Player names in lobby are now alpha sorted<!--END_RELEASE_NOTE-->
<!--RELEASE_NOTE-->NEW|Can right click player names to see info, including registration date.<!--END_RELEASE_NOTE-->
```

### Very long comments

If a comment is very long, just keep typing and let the text word-wrap to a next line. Do not include line breaks.

Example format for a very long note:

```
<!--RELEASE_NOTE-->CHANGE|This is a very long feature request comment.  This is a very long feature request comment.  This is a very long feature request comment. This is a very long feature request comment. This is a very long feature request comment.<!--END_RELEASE_NOTE-->
```

