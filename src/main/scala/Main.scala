import java.io.ByteArrayInputStream
import java.util

import akka.actor.ActorSystem
import akka.http.Http
import akka.http.Http.{IncomingConnection, ServerBinding}
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
      withInputStream(new ByteArrayInputStream(Md5Utils.computeMD5Hash(payload.toArray)))
  }

  val route: Route =
    path("file" / Segment) { key =>
      get {

//        val objects = client.listObjects(bucketName).getObjectSummaries
//        objects.foreach(e => {
//          println(e.getBucketName)
//          println(e.getKey)
//          println(e.getOwner)
//          println(e.getETag)
//        })



//        val o = client.getObject(bucketName, key)
//        val content:InputStream =  o.getObjectContent
//

//        val bytes = Source(ByteString(content))
//        val x2 = bytes.via(Flow[Byte].collect[ByteString]({
//          case x:Byte => ByteString(x)
//        }))
//
//        x2

//        Source(ByteString(content)).runWith(Sink.fold[HttpResponse, Byte](HttpResponse())((acc, payload) => {
//          e
//        }))

//        runFold[HttpResponse](HttpResponse())(e =>{
//
//        })

//        runWith(Sink.foreach())



//        Flow[InputStream](content).



//        content.close()

//
//        println("Content-Type: "  + o.getObjectMetadata().getContentType())

        complete("ok")
      } ~
      post { ctx =>

        val client = AmazonS3.init()
        val initRequest = new InitiateMultipartUploadRequest(bucketName, key)
        val initResponse = client.initiateMultipartUpload(initRequest)
        println("==============================")
        println(initResponse.getUploadId)
        println("==============================")

        ctx.request.entity.dataBytes.
          via(Flow[ByteString].grouped(2000)).
          runWith(Sink.fold[List[PartETag], Seq[ByteString]](List())((acc, chunkList) => {
          val payload = chunkList.flatten
          println("payload size is : " + payload.size)
          val part = acc match {

            case List() => client.uploadPart(chunk(key, payload, initResponse.getUploadId, 1))
            case h :: t => client.uploadPart(chunk(key, payload, initResponse.getUploadId, h.getPartNumber + 1))
          }
          println(part.getETag + " " + part.getPartNumber)
          println("===============================")
          part.getPartETag :: acc
        })).
        map(etags => {
          val tags =  new util.ArrayList[PartETag]()
          tags.addAll(etags)
          val compRequest = new CompleteMultipartUploadRequest(bucketName, key, initResponse.getUploadId, tags)
          try {
            val xxxx = client.completeMultipartUpload(compRequest)
            println("location " + xxxx.getLocation)
          } catch {
            case e:Exception =>
              println(e.getMessage)
              e.printStackTrace(System.out)
          }
          println("finished complete multipart request ")
        }).
        flatMap[RouteResult] { e =>
          println("complete http request")
          ctx.complete(s"result is")
        }
      }
    }

  val source: Source[IncomingConnection, Future[ServerBinding]] = Http().bind(interface = "localhost", port = 9000)
  source.to(Sink.foreach { connection =>
    println(connection.remoteAddress)
    connection handleWith Route.handlerFlow(route)
  }).run()

}
