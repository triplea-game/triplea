# Latest Version Module

This module provides a latest engine version web-API.  This would typically be
used by front-end clients to know if they are running an out of date engine version.

The latest version data is obtained from Github's webservice API. More specifically
we query Github's API for the 'tag name' of the 'latest release'. We then keep this
data in an in-memory cache and then refresh the cache periodically. In particular
we use a cache for a faster server response, but also to limit interactions with
Github's API (it is rate limited).
