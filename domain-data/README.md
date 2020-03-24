Sub-project to contain domain objects that have universal meaning across all
sub-projects.

Domain objects allow for a few things:
- code abstraction
- strong typing
- validation
- domain driven security
- domain driven design

Be careful about sub-domain specific entities vs the universal domain objects,
not all domain or sub-domain entities belong here.

This is a very low level sub-project that can be shared between DB
and client components for common objects that are shared between multiple
other sub-projects.

