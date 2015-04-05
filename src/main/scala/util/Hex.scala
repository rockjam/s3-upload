package util


trait Hex {
  def toHex(buf: Array[Byte]): String = buf.map("%02X" format _).mkString.toLowerCase
}
