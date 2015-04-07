package util

import akka.http.model.headers.CustomHeader

case class AmazonAuthorization(authString:String) extends CustomHeader {
  override def name = "Authorization"
  override def value = authString
}

case class `x-amz-content-sha256`(payloadHash:String) extends CustomHeader {
  override def name = "x-amz-content-sha256"
  override def value = payloadHash
}

case class `Transfer-Encoding`(encoding:String) extends CustomHeader {
  override def name = "Transfer-Encoding"
  override def value = encoding
}

case class ContentLengthCustom(length:Int) extends CustomHeader {
  override def name = "Content-Length"
  override def value = length.toString
}