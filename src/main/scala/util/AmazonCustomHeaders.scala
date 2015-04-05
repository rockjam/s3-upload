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
