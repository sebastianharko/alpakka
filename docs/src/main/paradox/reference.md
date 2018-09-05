# Reference

This is the reference documentation for an Alpakka connector. This section should contain
a general overview of the connector and mention the libraries and APIs that the connector
is using. Also it should link to external resources that might help to learn
about the technology the connector is using.

### Reported issues

[Tagged issues at Github](https://github.com/akka/alpakka/labels/p%3Areference)

## Artifacts

@@dependency [sbt,Maven,Gradle] {
  group=com.lightbend.akka
  artifact=akka-stream-alpakka-reference_$scalaBinaryVersion$
  version=$version$
}

## Reading messages

Give a brief description of the usage of this connector. If you want to mention a
class name, make sure to link to the API docs:
@scaladoc[ReferenceReadMessage](akka.stream.alpakka.reference.ReferenceReadMessage).

If any of the API classes are different between Scala and Java, link to both API docs:
@scala[@scaladoc[Reference](akka.stream.alpakka.reference.scaladsl.Reference$)]
@java[@scaladoc[Reference](akka.stream.alpakka.reference.javadsl.Reference$)].

Show an example code snippet of how a source of this connector can be created.

Scala
: @@snip [snip]($alpakka$/reference/src/test/scala/docs/scaladsl/ReferenceSpec.scala) { #source }

Java
: @@snip [snip]($alpakka$/reference/src/test/java/docs/javadsl/ReferenceTest.java) { #source }

Wrap language specific text with language specific directives,
like @scala[`@scala` for Scala specific text]@java[`@java` for Java specific text].

## Writing messages

Show an example code snippet of how a flow of this connector can be created.

Scala
: @@snip [snip]($alpakka$/reference/src/test/scala/docs/scaladsl/ReferenceSpec.scala) { #flow }

Java
: @@snip [snip]($alpakka$/reference/src/test/java/docs/javadsl/ReferenceTest.java) { #flow }

## Running the example code

The code in this guide is part of runnable tests of this project. You are welcome to edit the code and run it in sbt.

Scala
:   ```
    sbt
    > reference/testOnly *.ReferenceSpec
    ```
    
Java
:   ```
    sbt
    > reference/testOnly *.ReferenceTest
    ```
