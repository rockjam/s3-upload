package util

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

trait Crypto {

  def hash(text: String): Array[Byte] = {
    val md: MessageDigest = MessageDigest.getInstance("SHA-256")
    md.update(text.getBytes("UTF-8"))
    md.digest()
  }

  implicit def string2Byte(s:String): Array[Byte] = s.getBytes

  def sign(data: Array[Byte], key: Array[Byte]): Array[Byte] = {
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(new SecretKeySpec(key, "HmacSHA1"))
    mac.doFinal(data)
  }

}
