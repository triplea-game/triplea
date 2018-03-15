
See README.md in sub-projects for details on the individual projects.


![project_dependency](https://user-images.githubusercontent.com/12397753/36956914-da96d2ea-1fe5-11e8-92d9-9efd74bb1585.png)


## design goals

- Avoid direct dependency between game-core, client and the lobby
- lobby client interface provided by client-lobby will remain relatively stable
- integration test is done between lobby-client and lobby. From there on the
 game engine client can use a mock of lobby-client and avoid direct integration testing
 with lobby.