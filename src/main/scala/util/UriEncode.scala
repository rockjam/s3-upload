package util

import java.net.URLEncoder

trait UriEncode {

  def uriEncode(input:String, encodeSlash:Boolean = true):String = {
    input.foldLeft("")((acc, c) => {
      val enc = c match {
        case c: Char if (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '~' || c == '.' => c
        case '/' => if (encodeSlash) "%2F" else c
        case _ => encode(c)
      }
      acc + enc
    })
  }

  private def encode(c: Char): String = {
    URLEncoder.encode(c.toString, "UTF-8")
      .replace("+", "%20").replace("*", "%2A").replace("%7E", "~")
  }
}
