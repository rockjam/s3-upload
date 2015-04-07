package requests

import akka.http.model.headers.{Date, Host}
import akka.http.model.{HttpMethods, HttpProtocols, HttpRequest}
import akka.http.util.DateTime
import util._

case class InitMultipartUpload(key:String, reqDate:DateTime) extends Signing {

  val emptyHash = toHex(hash(""))
  override def date = reqDate
  override def hashedPayload = emptyHash

  def request(): HttpRequest = {
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = s"/$key?uploads",
      headers = List(
        Host(s"${Settings.bucketName}.s3.amazonaws.com"),
        Date(date),
        `x-amz-content-sha256`(emptyHash)
      ),
      protocol = HttpProtocols.`HTTP/1.1`
    )
    signRequest(request)
  }

}
