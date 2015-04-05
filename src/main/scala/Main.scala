import akka.actor.ActorSystem
import akka.http.Http
import akka.http.Http.{IncomingConnection, ServerBinding}
import akka.http.server.Directives._
import akka.http.server._
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.Future

object Main extends App {

  implicit val system = ActorSystem("s3-upload")
  implicit val materializer = ActorFlowMaterializer()
  implicit val executionContext = system.dispatcher


  val route: Route =
    path("file" / Segment) { key =>
      get {
        complete("ok")
      } ~
      post { ctx =>
        ctx.complete("ok")
      }
    }

  val source: Source[IncomingConnection, Future[ServerBinding]] = Http().bind(interface = "localhost", port = 9000)
  source.to(Sink.foreach { connection =>
    connection handleWith Route.handlerFlow(route)
  }).run()

}
