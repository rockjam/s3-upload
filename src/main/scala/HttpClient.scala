import akka.actor.ActorSystem
import akka.http.Http
import akka.http.model.{HttpRequest, HttpResponse}
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.{Sink, Source}
import util.Settings

import scala.concurrent.Future



//class HttpClient {
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