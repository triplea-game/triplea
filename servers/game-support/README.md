## Error Reporting Server - Overview

The error reporting service allows game clients to submit crash reports that are then entered
into Github Issues. For the most part the service is an intermediary between the TripleA UI
and the web-api for Github Issues.

The service provides throttling, it keeps tracks of error reports that have been submitted
and will allow only so many for a given window of time from a given user.
