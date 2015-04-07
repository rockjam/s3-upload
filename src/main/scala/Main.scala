import akka.actor.ActorSystem
import akka.http.Http
import akka.http.Http.{IncomingConnection, ServerBinding}
import akka.http.model.{HttpRequest, HttpHeader, HttpResponse}
import akka.http.model.headers.ETag
import akka.http.server.Directives._
import akka.http.server._
import akka.http.util.DateTime
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import requests._

import scala.concurrent.Future
import scala.util.{Failure, Success}

object Main extends App {

  implicit val system = ActorSystem("s3-upload")
  implicit val materializer = ActorFlowMaterializer()
  implicit val executionContext = system.dispatcher


//  case class Accumulator(etags:List[Option[String]], request:HttpRequest, partNumber:Int, signature:String)

  val route: Route =
    path("file" / Segment) { key =>
      get {
        complete("ok")
      } ~
      post { ctx =>
        val now = DateTime.now
        HttpClient.
          makeRequest(InitMultipartUpload(key, now)).
          //map / flatMap
          map { initResp =>
            initResp.entity.dataBytes.
              via(Flow[ByteString].map { e => e.decodeString("utf-8")/*.toXml.getUploadId*/}).//получаем в ответе uploadId
              runWith(Sink.head).
              map { upId =>
                ctx.request.
                  entity.
                  getDataBytes().
                  runWith(Sink.fold[(List[Option[String]], RequestingAndPartNumbering), ByteString]
                  (
                    (List[Option[String]](),First(key, upId, now))
                  )
                { (acc, payload) =>
                  val (etags, curr) = acc
                  val req = curr.request(payload)
                  val newAcc =
                  HttpClient.makeRequest(req._1).
                    collect {
                    case resp:HttpResponse =>
                      (resp.headers.find(_==ETag).map(_.value) :: etags,
                        Subsequent(key, upId, now, req._2, curr.partNumber + 1))
                  }.value
                  newAcc.get.get//получаем тут респонс, потому что нам он нужен
                }).
                map { acc =>
                  val (etags, last) = acc

                  //видимо last и есть последний перед
                  //только нет предыдущей signature
                  //поскольку
                  last.request(ByteString.empty)
                  HttpClient.makeRequest()



                  //последний запрос + закрытие upload-a
                  HttpClient.makeRequest(Final(key, upId, now, last.request(ByteString.empty)._2, last.partNumber + 1).request())


                }.value
              }


          }

        }







        ctx.complete("ok")
      }

  val source: Source[IncomingConnection, Future[ServerBinding]] = Http().bind(interface = "localhost", port = 9000)
  source.to(Sink.foreach { connection =>
    connection handleWith Route.handlerFlow(route)
  }).run()

}
