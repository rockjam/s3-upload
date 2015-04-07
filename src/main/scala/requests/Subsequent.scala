package requests

import akka.http.model.headers._
import akka.http.model.{HttpEntity, HttpProtocols, HttpMethods, HttpRequest}
import akka.http.util.DateTime
import akka.util.ByteString
import util.{`x-amz-content-sha256`, Settings}

case class Subsequent(key:String, uploadId:String, reqDate:DateTime, prevSignature:String, pNumber:Int)
  extends Signing with RequestingAndPartNumbering {

  override def date: DateTime = reqDate
  override def hashedPayload: String = ""//не используется
  override def partNumber = pNumber

  override def request(payload:ByteString):(HttpRequest, String) = {
    val signature = getSignature(stringToSign(payload))
    val request = HttpRequest(
      method = HttpMethods.PUT,
      uri = s"/$key?partNumber=$partNumber&uploadId=$uploadId",
      headers = List(
        Host(s"${Settings.bucketName}.s3.amazonaws.com"),
        Date(date),
        `Content-Encoding`(HttpEncoding.custom("aws-chunked")),
//        `Transfer-Encoding`("chunked"),
        Expect.`100-continue`,
        `x-amz-content-sha256`("STREAMING-AWS4-HMAC-SHA256-PAYLOAD")
      ),
      entity = HttpEntity(chunk(payload, signature)),
      protocol = HttpProtocols.`HTTP/1.1`
    )
    (request, signature)
  }

  def stringToSign(payload: ByteString) = {
    val scope = s"$fdate/${Settings.region}/s3/aws4_request"

    val emptyHash = toHex(hash(""))
    val hashedPayload = toHex(hash(payload.toArray))
    
    List(
      "AWS4-HMAC-SHA256-PAYLOAD",
      date.toRfc1123DateTimeString,
      scope,
      prevSignature,
      emptyHash,
      hashedPayload
    ).mkString("\n")
  }

  //дублирование
  def chunk(payload:ByteString, signature:String):ByteString = {
    val chunkSize = payload.length.toHexString
    ByteString(chunkSize + ";chunk-signature=" + signature + "\r\n") ++ payload ++ ByteString("\r\n")
  }



}