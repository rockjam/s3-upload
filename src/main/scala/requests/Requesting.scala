package requests

import akka.http.model.HttpRequest
import akka.util.ByteString

trait Requesting {
  //rename
  def request(payload:ByteString):(HttpRequest, String)
}
