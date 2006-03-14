#!/bin/sh

# Mac OS X handles appbundle icons very strangely
#
# The icon file has to be called Icon^M,
#     where the ^M is really a carriage return
#     (I think it's a carriage return, it's a \r)
#
# Ant doesn't seem to play nice with this strange encoding,
#     so run this file after running the macRelease ant target

mv TripleA.app/Icon TripleA.app/Icon^M