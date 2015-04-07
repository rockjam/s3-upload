package util

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

trait Crypto {

  def hash(text: String): Array[Byte] = hash(text.getBytes("UTF-8"))

  def hash(bytes:Array[Byte]): Array[Byte] = {
    val md: MessageDigest = MessageDigest.getInstance("SHA-256")
    md.update(bytes)
    md.digest()
  }

  implicit def string2Byte(s:String): Array[Byte] = s.getBytes

  def sign(data: Array[Byte], key: Array[Byte]): Array[Byte] = {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(new SecretKeySpec(key, "HmacSHA256"))
    mac.doFinal(data)
  }

}
