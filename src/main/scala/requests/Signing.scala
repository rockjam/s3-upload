package requests

import akka.http.model.{HttpHeader, HttpRequest}
import akka.http.util.DateTime
import util._

trait Signing extends UriEncode with Crypto with Hex {

  def date:DateTime
  def hashedPayload:String
  def fdate = date.toIsoDateString().replaceAll("-", "")

  def signRequest(request:HttpRequest):HttpRequest = {
    request.addHeader(AmazonAuthorization(authString(request)))
  }

  def authString(request: HttpRequest) = {
    val signedHeaders = getSignedHeaders(request.headers)
    val signature = getSignature(getStringToSign(getCanonicalRequest(request)))
    List(
      s"AWS4-HMAC-SHA256 Credential=${Settings.accessKeyId}/$fdate/${Settings.region}/s3/aws4_request",
      s"SignedHeaders=$signedHeaders",
      s"Signature=$signature"
    ) mkString ", "
  }

  def getSignature(stringToSign:String) = {
    val DateKey = sign(fdate, s"AWS4${Settings.secretAccessKey}")
    val DateRegionKey = sign(Settings.region, DateKey)
    val DateRegionServiceKey = sign("s3", DateRegionKey)
    val SigningKey = sign("aws4_request", DateRegionServiceKey)

    toHex(sign(stringToSign, SigningKey))
  }

  def getStringToSign(canonicalRequest:String) = {
    val scope = s"$fdate/${Settings.region}/s3/aws4_request"
    val hashed = toHex(hash(canonicalRequest))
    List(
      "AWS4-HMAC-SHA256",
      date.toRfc1123DateTimeString,
      scope,
      hashed
    ).mkString("\n")
  }

  def getCanonicalRequest(request: HttpRequest) = {
    val HTTPMethod = request.method.name
    val CanonicalURI = uriEncode(request.getUri().path(), false)
    val CanonicalQueryString = getCanonicalQueryString(request.getUri().queryString())
    val CanonicalHeaders = getCanonicalHeaders(request.headers)
    val SignedHeaders = getSignedHeaders(request.headers)
    val HashedPayload = hashedPayload
    List(
      HTTPMethod,
      CanonicalURI,
      CanonicalQueryString,
      CanonicalHeaders,
      SignedHeaders,
      HashedPayload
    ).mkString("\n")
  }

  def getCanonicalQueryString(queryString:String) =
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

  def getCanonicalHeaders(headers:Seq[HttpHeader]) =
    headers.
      map(h => s"${h.lowercaseName()}:${h.value().trim}").
      sorted.
      mkString("\n")+"\n"

  def getSignedHeaders(headers:Seq[HttpHeader]) =
    headers.
      map(_.lowercaseName).
      sorted.
      mkString(";")

}
