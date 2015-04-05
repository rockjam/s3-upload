import akka.actor.ActorSystem
import akka.http.Http
import akka.http.model.{HttpRequest, HttpResponse}
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.Future



object HttpClient {

  def makeRequest(request: HttpRequest)(implicit system: ActorSystem, materializer: FlowMaterializer): Future[HttpResponse] = {
    implicit val ec = system.dispatcher
    val flow = Http().outgoingConnection(s"${Settings.bucketName}.s3-${Settings.region}.amazonaws.com")
    Source.
      single(request).
      via(flow).
      runWith(Sink.head)
  }
}

//val z = Source.
//  single(request).
//  via(flow).
//  runWith(Sink.head).
//  andThen {
//  case Success(r) =>
//    r.encoding
//    println(r.entity.contentType())
//    println(r.entity.toString)
//    println(r.headers.foreach(println))
//    //          println(
//    //            r.
//    //              entity.
//    //              dataBytes.
//    //              via(Flow[ByteString].map(_.decodeString("utf-8"))).
//    //              runWith(Sink.head).andThen{
//    //                case Success(s) => println(s)
//    //                case Failure(e) => println(e)
//    //              }
//    //          )
//    println(r.entity.dataBytes.via(Flow[ByteString].map(_.length)).runWith(Sink.head).andThen{
//      case Success(r) => println(r)
//      case Failure(s) => println(s)
//    })
//
//  case Failure(e) =>
//    println("failure")
//}
