package requests

import akka.http.model.headers.{Date, Host}
import akka.http.model.{HttpMethods, HttpProtocols, HttpRequest}
import akka.http.util.DateTime
import util._

object InitMultipartUpload {
  def apply(key:String, date:DateTime = DateTime.now) = new InitMultipartUpload(date).request(key)
}

class InitMultipartUpload(val reqDate:DateTime) extends Signing {

  val emptyHash = toHex(hash(""))
  override def date = reqDate
  override def hashedPayload = emptyHash

  def request(key: String): HttpRequest = {
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
