# play-silhouette-persistence-datomic [![Build Status](https://travis-ci.org/Enalmada/play-silhouette-persistence-datomic.svg?branch=master)](https://travis-ci.org/Enalmada/play-silhouette-persistence-datomic) [![Join the chat at https://gitter.im/Enalmada/play-silhouette-persistence-datomic](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/Enalmada/play-silhouette-persistence-datomic?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.enalmada/play-silhouette-persistence-datomic/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.enalmada/play-silhouette-persistence-datomic)

Play silhouette datomic persistence.
Consult the [Silhouette documentation](http://silhouette.mohiva.com/docs) for more information. If you need help with the integration of Silhouette into your project, don't hesitate and ask questions in our [mailing list](https://groups.google.com/forum/#!forum/play-silhouette) or on [Stack Overflow](http://stackoverflow.com/questions/tagged/playframework).

#### Version information
* `2.5.0` to `2.5.x` (last: `0.1.0` - [master branch](https://github.com/enalmada/play-silhouette-persistence-datomic/tree/master))

Releases are on [mvnrepository](http://mvnrepository.com/artifact/com.github.enalmada) and snapshots can be found on [sonatype](https://oss.sonatype.org/content/repositories/snapshots/com/github/enalmada).

## Quickstart
Clone the project and run `sbt run` to see a sample application.

### To test Rest
curl -X POST http://localhost:9000/rest/signIn -H 'Content-Type: application/json' -d '{"email": "enalmada@gmail.com", "password": "bla!", "rememberMe": true}'


### Including the Dependencies

```xml
<dependency>
    <groupId>com.github.enalmada</groupId>
    <artifactId>play-silhouette-persistence-datomic_2.11</artifactId>
    <version>0.1.0</version>
</dependency>
```
or

```scala
val appDependencies = Seq(
  "com.github.enalmada" %% "play-silhouette-persistence-datomic" % "0.1.0"
)
```

## Versions
* **TRUNK** [not released in the repository, yet]
  * Fancy contributing something? :-)
* **0.1.0** [release on 2016-04-12]
  * Initial release.

## TODO (help out!)


## License

Copyright (c) 2016 Adam Lane

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this work except in compliance with the License. You may obtain a copy of the License in the LICENSE file, or at:

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

