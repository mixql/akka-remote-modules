import akka.serialization.SerializerWithStringManifest

import java.nio.charset.StandardCharsets
import TestMsgs._
import akka.actor.{ActorRef, ExtendedActorSystem}
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import akka.serialization._
import io.circe._, io.circe.parser._
import cats.syntax.either._


class CustomCirceSerialisation(system: ExtendedActorSystem ) extends Serializer {

  val UTF_8 = StandardCharsets.UTF_8.name()

  // Pick a unique identifier for your Serializer,
  // you've got a couple of billions to choose from,
  // 0 - 40 is reserved by Akka itself
  def identifier = 1234567

  def includeManifest = true

  // The manifest (type hint) that will be provided in the fromBinary method
  // Use `""` if manifest is not needed.
  //  def manifest(obj: AnyRef): String =
  //    obj match
  //      case _: shared.Shutdown => "Shutdown"
  //      case _: List[String]     => "List[String]"
  //      case _: AkkaMsgTest1     => "AkkaMsgTest1"
  //      case _: AkkaMsgTest2     => "AkkaMsgTest2"
  //      case _: AkkaMsgTestActorRef     => "AkkaMsgTestActorRef"



  implicit val encodeActorRef: Encoder[ActorRef] = new Encoder[ActorRef] {
    final def apply(theActorRef: ActorRef): Json = Json.obj(
      ("ActorRef", Json.fromString(Serialization.serializedActorPath(theActorRef)))
    )
  }
  implicit val decodeActorRef: Decoder[ActorRef] = new Decoder[ActorRef] {
    final def apply(c: HCursor): Decoder.Result[ActorRef] =
      c.downField("ActorRef").as[String].map(
        path => ModuleScala2_11Run.backend.asInstanceOf[ExtendedActorSystem].provider.resolveActorRef(path)
      )

  }


  // "toBinary" serializes the given object to an Array of Bytes
  def toBinary(obj: AnyRef): Array[Byte] = {
    // Put the real code that serializes the object here

    obj match {
      case a: shared.Shutdown => a.asJson.noSpaces.getBytes(UTF_8)
      case msg: AkkaMsgTest1 => msg.asJson.noSpaces.getBytes(UTF_8)
      case msg: AkkaMsgTestActorRef => msg.asJson.noSpaces.getBytes(UTF_8)
    }
  }

  // "fromBinary" deserializes the given array,
  // using the type hint
  def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {

    // Put the real code that deserializes here
    manifest.get.getName match {
      case "shared.Shutdown" =>
        parse(new String(bytes, UTF_8)).getOrElse(Json.Null).as[shared.Shutdown].toTry.get
      case "TestMsgs.AkkaMsgTest1" =>
        parse(new String(bytes, UTF_8)).getOrElse(Json.Null).as[AkkaMsgTest1].toTry.get
      case "TestMsgs.AkkaMsgTestActorRef" =>
        parse(new String(bytes, UTF_8)).getOrElse(Json.Null).as[AkkaMsgTestActorRef].toTry.get
    }
  }
}