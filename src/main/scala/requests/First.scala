package requests

import akka.http.model._
import akka.http.model.headers._

import akka.http.util.DateTime
import akka.util.ByteString
import util.{`Transfer-Encoding`, `x-amz-content-sha256`, Settings}

case class First(key:String, uploadId:String, reqDate:DateTime)
  extends Signing with Requesting {

  override def date = reqDate
  override def hashedPayload = "STREAMING-AWS4-HMAC-SHA256-PAYLOAD"

  override def request(payload:ByteString):(HttpRequest, String) = {
//    val fakeContent = chunk(payload, toHex(sign("", "1")))

    val request = HttpRequest(
      method = HttpMethods.PUT,
      uri = s"/$key?partNumber=1&uploadId=$uploadId",
      headers = List(
        Host(s"${Settings.bucketName}.s3.amazonaws.com"),
        Date(date),
        `Content-Encoding`(HttpEncoding.custom("aws-chunked")),
        `Transfer-Encoding`("chunked"),
        Expect.`100-continue`,
        `x-amz-content-sha256`("STREAMING-AWS4-HMAC-SHA256-PAYLOAD")
      ),
//      entity = HttpEntity(fakeContent),
      protocol = HttpProtocols.`HTTP/1.1`
    )
    val signedRequest = signRequest(request)
    val signature = seedSignature(request)

    (signedRequest.withEntity(chunk(payload, signature)), signature)
  }

  def seedSignature(request: HttpRequest): String = {
    getSignature(getStringToSign(getCanonicalRequest(request)))
  }

  def chunk(payload:ByteString, signature:String):ByteString = {
    val chunkSize = payload.length.toHexString
    ByteString(chunkSize + ";chunk-signature=" + signature + "\r\n") ++ payload ++ ByteString("\r\n")
  }

}