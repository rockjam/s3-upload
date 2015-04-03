import java.io._
import java.util

import akka.actor.ActorSystem
import akka.http.Http
import akka.http.Http.{IncomingConnection, ServerBinding}
import akka.http.model._
import akka.http.model.headers.`Transfer-Encoding`
import akka.http.server.Directives._
import akka.http.server._
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import com.amazonaws.services.s3.model._
import com.amazonaws.util.Md5Utils

import scala.collection.JavaConversions._
import scala.concurrent.Future

object Main extends App {

  implicit val system = ActorSystem("s3-upload")
  implicit val materializer = ActorFlowMaterializer()
  implicit val executionContext = system.dispatcher
  val bucketName = "actor-s3-test"

  def chunk(key: String, payload: Seq[Byte], uploadId: String, partNumber: Int) = {
    new UploadPartRequest().
      withBucketName(bucketName).
      withKey(key).
      withUploadId(uploadId).
      withPartNumber(partNumber).
      withPartSize(payload.length).
      withMD5Digest(Md5Utils.md5AsBase64(payload.toArray)).
      withInputStream(new ByteArrayInputStream(payload.toArray))

    //      withInputStream(new StringInputStream(Hex.encodeHexString(payload.toArray)))
    //      withInputStream(new ByteArrayInputStream(Hex.
    //      encodeHexString(payload.toArray).
    //      getBytes(StandardCharsets.UTF_8)))
  }

  val route: Route =
    path("file" / Segment) { key =>
      get {
        val client = AmazonS3.init()

        val o = client.getObject(bucketName, key)
        val length = o.getObjectMetadata.getContentLength()
        val content = o.getObjectContent

        val bytes = new Array[Byte](length.toInt)
        content.read(bytes)
        Source[Byte](bytes.toList).

          via(Flow[Byte].grouped(bytes.size / 5)).
          to(Sink.foreach { e =>
          //          println("payload is " + e.size)
          println(e)
          complete(HttpResponse(
            headers = List(`Transfer-Encoding`(TransferEncodings.chunked)),
            entity = HttpEntity(e.toArray)
          ))
        }).
          run()

        complete("ok")
      } ~
      post { ctx =>

        val client = AmazonS3.init()
        val initRequest = new InitiateMultipartUploadRequest(bucketName, key)
        val initResponse = client.initiateMultipartUpload(initRequest)

        ctx.request.entity.dataBytes.
          via(Flow[ByteString].grouped(1000)).
          runWith(Sink.fold[List[PartETag], Seq[ByteString]](List())((acc, chunkList) => {
          val payload = chunkList.flatten
          val part = acc match {

            case List() => client.uploadPart(chunk(key, payload, initResponse.getUploadId, 1))
            case h :: t => client.uploadPart(chunk(key, payload, initResponse.getUploadId, h.getPartNumber + 1))
          }
          part.getPartETag :: acc
        })).
          map(etags => {
          val tags = new util.ArrayList[PartETag]()
          tags.addAll(etags)
          val compRequest = new CompleteMultipartUploadRequest(bucketName, key, initResponse.getUploadId, tags)
          try {
            client.completeMultipartUpload(compRequest)
          } catch {
            case e: Exception =>
              println(e.getMessage)
          }
        }).
          flatMap[RouteResult] { e =>
          ctx.complete(HttpResponse(
            status = 200,
            entity = HttpEntity("Upload complete")
          ))
        }
      }
    }

  val source: Source[IncomingConnection, Future[ServerBinding]] = Http().bind(interface = "localhost", port = 9000)
  source.to(Sink.foreach { connection =>
    connection handleWith Route.handlerFlow(route)
  }).run()

}
