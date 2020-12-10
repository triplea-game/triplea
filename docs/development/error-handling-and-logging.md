# Error Handling And Logging

This document describes the desired general strategy for error handling in TripleA.

In short, TripleA is a thick client, we need to try to handle errors as much as possible
and when we cannot, the audience for the error message is the game-playing user. Errors
generally should be true game crashes, not just the unhappy path of code. For example,
if a user has no network, does not select the right file type, those are not errors.

Error and warning messages are intended for users, should be  quite descriptive in telling
them what happened (in non-technical language) and what they can do to fix the problem.
Most users will assume any message they receive means they need to restart the app,
so try to help them out as much as possible.

## Error Notifications and Pop-ups

- Severe logging will be displayed in a swing dialog with a "report to triplea" button. Use
this for unrecoverable and true game crashes. We'll strive to fix these.

- Warning logging will only be displayed in a swing dialog. Use this to notify users
of expected problems, like "no network available," "could not open port 3300, port is
already in use." These should be problems that a user would have likely triggered
by for example developing invalid XML, their system not supporting something, and
should likely be something they can fix.

- Info logging is not displayed to user, it is sent to the 'console' that can be
optionally made visible by a user but is not by default. Typically info logging
should be avoided, most interesting messages will be warning or severe messages.

