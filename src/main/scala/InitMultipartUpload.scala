import akka.http.model.headers.{Date, Host}
import akka.http.model.{HttpHeader, HttpMethods, HttpProtocols, HttpRequest}
import akka.http.util.DateTime
import util.{AmazonAuthorization, `x-amz-content-sha256`, Crypto, Hex, UriEncode}

object InitMultipartUpload {
  def apply(key:String) = new InitMultipartUpload().request(key)
}

class InitMultipartUpload extends UriEncode with Crypto with Hex {

  private val date = DateTime.now
  //date formatted as yyyyMMdd
  private val fdate = date.toIsoDateString().replaceAll("-", "")

  def request(key: String): HttpRequest = {
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = s"/$key?uploads",
      headers = List(
        Host(s"${Settings.bucketName}.s3.amazonaws.com"),
        Date(date),
        `x-amz-content-sha256`(toHex(hash("")))
      ),
      protocol = HttpProtocols.`HTTP/1.1` //нужно ли??
    )
    val authString = getAuth(request)
    request.addHeader(AmazonAuthorization(authString))
  }


  private def getAuth(request: HttpRequest) = {
    val signedHeaders = getSignedHeaders(request.headers)
    val signature = getSignature(getStringToSign(getCanonicalRequest(request)))
    List(
      s"AWS4-HMAC-SHA256 Credential=${Settings.accessKeyId}/$fdate/${Settings.region}/s3/aws4_request",
      s"SignedHeaders=$signedHeaders",
      s"Signature=$signature"
    ) mkString ", "
  }

  private def getSignature(stringToSign:String) = {
    val DateKey = sign(s"AWS4${Settings.secretAccessKey}", fdate)
    val DateRegionKey = sign(DateKey, Settings.region)
    val DateRegionServiceKey = sign(DateRegionKey, "s3")
    val SigningKey = sign(DateRegionServiceKey, "aws4_request")
    
    sign(SigningKey, stringToSign)
  }
  
  private def getStringToSign(canonicalRequest:String) = {
    val timeStampISO8601Format = DateTime.now.toIsoDateString().replaceAll("-","").replaceAll(":","")+"Z"
    val scope = s"$fdate/${Settings.region}/s3/aws4_request"
    val hashed = toHex(hash(canonicalRequest))
    s"""
      |AWS4-HMAC-SHA256
      |$timeStampISO8601Format
      |$scope
      |$hashed
    """.stripMargin
  }

  private def getCanonicalRequest(request: HttpRequest) = {
    val HTTPMethod = request.method.name
    val CanonicalURI = uriEncode(request.getUri().path(), false)
    val CanonicalQueryString = getCanonicalQueryString(request.getUri().queryString())
    val CanonicalHeaders = getCanonicalHeaders(request.headers)
    val SignedHeaders = getSignedHeaders(request.headers)
    val HashedPayload = toHex(hash(""))
    s"""
        |$HTTPMethod
        |$CanonicalURI
        |$CanonicalQueryString
        |$CanonicalHeaders
        |$SignedHeaders
        |$HashedPayload
    """.stripMargin
  }

  private def getCanonicalQueryString(queryString:String) =
    queryString.
      split('&').map { e =>
        e.split('=') match {
          case Array(k, v) => uriEncode(k) + "=" + uriEncode(v)
          case Array(k) => uriEncode(k) + "=" + ""
          case _ => ""
        }
      }.
      sorted.
      mkString("&")

  private def getCanonicalHeaders(headers:Seq[HttpHeader]) =
    headers.
      map(h => s"${h.lowercaseName()}:${h.value().trim}").
      sorted.
      mkString("\n")

  private def getSignedHeaders(headers:Seq[HttpHeader]) =
    headers.
      map(_.lowercaseName).
      sorted.
      mkString(";")

}
