import akka.actor.ActorSystem
import akka.http.Http
import akka.http.Http.{IncomingConnection, ServerBinding}
import akka.http.model.HttpResponse
import akka.http.model.headers.ETag
import akka.http.server.Directives._
import akka.http.server._
import akka.http.util.DateTime
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import requests._

import scala.concurrent.Future
import scala.xml.XML

object Main extends App {

  implicit val system = ActorSystem("s3-upload")
  implicit val materializer = ActorFlowMaterializer()
  implicit val executionContext = system.dispatcher


  case class Accumulator(etags:List[Option[String]], request:Requesting, partNumber:Int) {
    def nextPart = partNumber + 1
  }

  val route: Route =
    path("file" / Segment) { key =>
      get {
        complete("ok")
      } ~
      post { ctx =>
        val now = DateTime.now
        val client = new HttpClient()
        client.
          makeRequest(InitMultipartUpload(key, now).request()).
          flatMap { initResp =>
            initResp.entity.dataBytes.
              via(Flow[ByteString].map { e => XML.loadString(e.decodeString("utf-8")).
                child.find(_.label=="UploadId").map(_.text).get//remove get
              }).
              runWith(Sink.head).
              flatMap { upId =>
                ctx.request.
                  entity.
                  dataBytes.
                  runWith(Sink.fold[Future[Accumulator], ByteString]
                    (Future(Accumulator(List(), First(key, upId, now), 1)))
                    { (af, payload) =>
                      af.flatMap { acc =>
                        val req = acc.request.request(payload)
                        client.makeRequest(req._1).
                          map { resp:HttpResponse =>
                          Accumulator(
                            resp.headers.find(_==ETag).map(_.value) :: acc.etags,
                            Subsequent(key, upId, now, req._2, acc.nextPart),
                            acc.nextPart)
                          }
                      }
                    }).
                    flatMap { last =>
                      last.flatMap { acc =>
                        val request = acc.request.request(ByteString.empty)._1
                        client.
                          makeRequest(request).
                          flatMap { resp:HttpResponse =>
                            val etags = (resp.headers.find(_==ETag).map(_.value) :: acc.etags).flatten
                            val completeRequest = CompleteMultipartUpload(key, upId, now, etags).request()
                            client.makeRequest(completeRequest).
                              flatMap {resp:HttpResponse =>
                                resp.entity.dataBytes.
                                  via(Flow[ByteString].map(_.decodeString("utf-8"))).
                                  runWith(Sink.head).
                                  flatMap { body =>
                                    ctx.complete(HttpResponse(status = resp.status, entity = body))
                                  }
                              }
                          }
                      }
                    }
              }
          }
        }
      }

  val source: Source[IncomingConnection, Future[ServerBinding]] = Http().bind(interface = "localhost", port = 9000)
  source.to(Sink.foreach { connection =>
    connection handleWith Route.handlerFlow(route)
  }).run()

}
