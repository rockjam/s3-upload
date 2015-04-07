package requests

import akka.http.model.headers.{Date, Host}
import akka.http.model.{HttpEntity, HttpMethods, HttpProtocols, HttpRequest}
import akka.http.util.DateTime
import util.Settings

case class CompleteMultipartUpload(key:String, uploadId:String, reqDate:DateTime, etags:List[String])
  extends Signing {

  override def date: DateTime = reqDate
  override def hashedPayload: String = toHex(hash(body))

  def request():HttpRequest = {
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = s"/$key?uploadId=$uploadId",
      headers = List(
        Host(s"${Settings.bucketName}.s3.amazonaws.com"),
        Date(date)
      ),
      entity = HttpEntity(body),
      protocol = HttpProtocols.`HTTP/1.1`
    )
    signRequest(request)
  }

  private val body:String = {
    val b = etags.
      zipWithIndex.
      map { e =>
        val (etag, i) = e
        s"""<Part>
          | <PartNumber>${i+1}</PartNumber>
          | <ETag>$etag</ETag>
          |</Part>""".stripMargin
      }.mkString("\n")
    s"""<CompleteMultipartUpload>
        |$b
        |</CompleteMultipartUpload>""".stripMargin
  }


}
