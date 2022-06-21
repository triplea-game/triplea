# Map Loading Design

Document describes the design behind game parsing and how games are transformed
from XML to Java Code

## Parsing

XML parsing is done in the 'xml-reader' sub-project. This has the responsibility of just
reading the data in the XML and converting it to a POJO.

The 'map-data' sub-project stores the POJOs that model the data in XML. These POJOs
should have no semantic information about the information read, just model what
was in the XML. For example instead of creating java game objects around properties
and options, the POJOs will simply record property and options field values without
knowing what they mean, just model what was in the XML.

## Assembly

Assembly is the process of transforming 'map-data' POJOs into a game object. This is where
we add semantic meaning to the data that was read. This creates a two step process and allows
flexibility for there to be a variety of XMLs formats that can all be assembled in the same
way.

To illustrate the data transformation, it looks something like this:
![map-data-flow](https://user-images.githubusercontent.com/12397753/91668327-d0726600-eac0-11ea-98b5-d10054337c60.png)

