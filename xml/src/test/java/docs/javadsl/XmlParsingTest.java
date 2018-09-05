/*
 * Copyright (C) 2016-2018 Lightbend Inc. <http://www.lightbend.com>
 */

package docs.javadsl;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.alpakka.xml.*;
import akka.stream.alpakka.xml.javadsl.XmlParsing;
import akka.stream.javadsl.*;
import akka.testkit.javadsl.TestKit;
import akka.util.ByteString;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;

public class XmlParsingTest {
  private static ActorSystem system;
  private static Materializer materializer;

  @Test
  public void xmlParser() throws InterruptedException, ExecutionException, TimeoutException {

    // #parser
    final Sink<String, CompletionStage<List<ParseEvent>>> parse =
        Flow.<String>create()
            .map(ByteString::fromString)
            .via(XmlParsing.parser())
            .toMat(Sink.seq(), Keep.right());
    // #parser

    // #parser-usage
    final String doc = "<doc><elem>elem1</elem><elem>elem2</elem></doc>";
    final CompletionStage<List<ParseEvent>> resultStage =
        Source.single(doc).runWith(parse, materializer);
    // #parser-usage

    resultStage
        .thenAccept(
            (list) -> {
              assertThat(
                  list,
                  hasItems(
                      StartDocument.getInstance(),
                      StartElement.create("doc", Collections.emptyMap()),
                      StartElement.create("elem", Collections.emptyMap()),
                      Characters.create("elem1"),
                      EndElement.create("elem"),
                      StartElement.create("elem", Collections.emptyMap()),
                      Characters.create("elem2"),
                      EndElement.create("elem"),
                      EndElement.create("doc"),
                      EndDocument.getInstance()));
            })
        .toCompletableFuture()
        .get(5, TimeUnit.SECONDS);
  }

  @Test
  public void xmlSubslice() throws InterruptedException, ExecutionException, TimeoutException {

    // #subslice
    final Sink<String, CompletionStage<List<ParseEvent>>> parse =
        Flow.<String>create()
            .map(ByteString::fromString)
            .via(XmlParsing.parser())
            .via(XmlParsing.subslice(Arrays.asList("doc", "elem", "item")))
            .toMat(Sink.seq(), Keep.right());
    // #subslice

    // #subslice-usage
    final String doc =
        "<doc>"
            + "  <elem>"
            + "    <item>i1</item>"
            + "    <item><sub>i2</sub></item>"
            + "    <item>i3</item>"
            + "  </elem>"
            + "</doc>";
    final CompletionStage<List<ParseEvent>> resultStage =
        Source.single(doc).runWith(parse, materializer);
    // #subslice-usage

    resultStage
        .thenAccept(
            (list) -> {
              assertThat(
                  list,
                  hasItems(
                      Characters.create("i1"),
                      StartElement.create("sub", Collections.emptyMap()),
                      Characters.create("i2"),
                      EndElement.create("sub"),
                      Characters.create("i3")));
            })
        .toCompletableFuture()
        .get(5, TimeUnit.SECONDS);
  }

  @Test
  public void xmlSubtree() throws InterruptedException, ExecutionException, TimeoutException {

    // #subtree
    final Sink<String, CompletionStage<List<Element>>> parse =
        Flow.<String>create()
            .map(ByteString::fromString)
            .via(XmlParsing.parser())
            .via(XmlParsing.subtree(Arrays.asList("doc", "elem", "item")))
            .toMat(Sink.seq(), Keep.right());
    // #subtree

    // #subtree-usage
    final String doc =
        "<doc>"
            + "  <elem>"
            + "    <item>i1</item>"
            + "    <item><sub>i2</sub></item>"
            + "    <item>i3</item>"
            + "  </elem>"
            + "</doc>";
    final CompletionStage<List<Element>> resultStage =
        Source.single(doc).runWith(parse, materializer);
    // #subtree-usage

    resultStage
        .thenAccept(
            (list) -> {
              assertThat(
                  list.stream().map(e -> XmlHelper.asString(e).trim()).collect(Collectors.toList()),
                  hasItems("<item>i1</item>", "<item><sub>i2</sub></item>", "<item>i3</item>"));
            })
        .toCompletableFuture()
        .get(5, TimeUnit.SECONDS);
  }

  @BeforeClass
  public static void setup() throws Exception {
    system = ActorSystem.create();
    materializer = ActorMaterializer.create(system);
  }

  @AfterClass
  public static void teardown() throws Exception {
    TestKit.shutdownActorSystem(system);
  }
}
