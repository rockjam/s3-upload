import java.io.ByteArrayInputStream

import akka.actor.ActorSystem
import akka.http.Http
import akka.http.Http.{IncomingConnection, ServerBinding}
import akka.http.server.Directives._
import akka.http.server._
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import com.amazonaws.services.s3.model._

import scala.concurrent.Future

object Main extends App {

  implicit val system = ActorSystem("s3-upload")
  implicit val materializer = ActorFlowMaterializer()
  implicit val executionContext = system.dispatcher
  val bucketName = "actor-s3-test"

  def flow(fileName: String): Flow[ByteString, ByteString, Unit] = Flow[ByteString].map(p => {
    println(fileName)
    println(p)
    p
  })

//  Source(1 to 6).via(Flow[Int].map(e => e * 2)).to(Sink.foreach(println(_)))

  def chunk(key: String, payload: ByteString, uploadId: String, partNumber: Int) = {
    new UploadPartRequest().
      withBucketName(bucketName).
      withKey(key).
      withUploadId(uploadId).
      withPartNumber(partNumber).
      withInputStream(new ByteArrayInputStream(payload.toArray))
  }


  val route: Route =
    path("hello" / IntNumber) { id =>
      get { ctx =>
        ctx.complete {

          ctx.request.getUri() + " hello world " + id
        }
      }
    } ~
    path("file" / Segment) { key =>
      get {
//        val o = client.getObject(new GetObjectRequest(bucketName, key))
//
//        println("Content-Type: "  + o.getObjectMetadata().getContentType())
        complete("ok")
      } ~
      post {
        ctx =>
        val client = AmazonS3.init()

        println("str is: " + key)
        //      requestContext.request.entity.dataBytes.via(Flow[ByteString]).runForeach(e => upload(key))
        val initRequest = new InitiateMultipartUploadRequest(bucketName, key)
        val initResponse = client.initiateMultipartUpload(initRequest)
        println("==============================")
        println(initResponse.getUploadId)
        println("==============================")

        val x = ctx.request.entity.dataBytes.
          runWith(Sink.fold[List[PartETag], ByteString](List())((acc, payload) => {
          println("payload size is : " + payload.size)
          val part = acc match {
            case List() => client.uploadPart(chunk(key, payload, initResponse.getUploadId, 1))
            case h :: t => client.uploadPart(chunk(key, payload, initResponse.getUploadId, h.getPartNumber + 1))
          }
          println(part.getETag + " " + part.getPartNumber)
          part.getPartETag :: acc
        })).
        map(e => {
          import scala.collection.JavaConversions._

          val compRequest:CompleteMultipartUploadRequest  = new
              CompleteMultipartUploadRequest(
                bucketName,
                key,
                initResponse.getUploadId,
                e)

          val zzz = client.completeMultipartUpload(compRequest)


//          val compRequest:CompleteMultipartUploadRequest = new CompleteMultipartUploadRequest(bucketName, key, initResponse.getUploadId, e)

//          println("up id " + compRequest.getUploadId)
//          println("bucket " + compRequest.getBucketName)
//          println("key " + compRequest.getKey)
//          e.foreach(println(_))
//          println("client " + client)
//          println(client.listMultipartUploads(new ListMultipartUploadsRequest(bucketName)))
//          val zzz = client.completeMultipartUpload(compRequest)
//          println(zzz.getKey)
          println("finished complete multipart request " + zzz)
        })
//        onComplete(x) ({
//          case Success() => complete(s"result is")
//          case Failure() => complete("not ok")
//        })
          ctx.complete(s"result is")
      }
    }


  //  val handler: HttpRequest => HttpResponse = {
//    case HttpRequest(HttpMethods.POST, Uri.Path("/file/dump"), _, e:HttpEntity.Chunked, _) =>
//      println(e.dataBytes)
//      HttpResponse(100)
//    case _ =>
//      println("=======================ERROROOOORR====================")
//      HttpResponse(404)
//  }

  val source: Source[IncomingConnection, Future[ServerBinding]] = Http().bind(interface = "localhost", port = 9000)
  source.to(Sink.foreach { connection =>
    println(connection.remoteAddress)
    connection handleWith Route.handlerFlow(route)
//    connection handleWithSyncHandler handler
  }).run()

}
