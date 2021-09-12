# Infrastructure Project

Hosts the code and configuration that controls deployments.

Design and conventions is in the [/infrastructure/docs](./docs) folder.


## .orig files

Any configuration files we will try to save copy of the default, original file that comes out of the box
and this file will be suffixed with '.orig'. By comparing the .orig file with the one we have templated
or saved to deploy, we can then see which changes we have made compared to the defaults.
