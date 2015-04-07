package requests

import akka.http.model.HttpRequest
import akka.http.util.DateTime
import akka.util.ByteString

case class Final(key:String, uploadId:String, reqDate:DateTime, prevSignature:String, pNumber:Int)
  extends Signing with RequestingAndPartNumbering {
  override def date: DateTime = ???

  override def hashedPayload: String = ???

  override def request(payload: ByteString): HttpRequest = ???

  override def partNumber: Int = ???
}
