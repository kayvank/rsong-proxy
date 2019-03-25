package coop.rchain.utils

import org.specs2._

class HexUtilsSpec extends Specification {
  def is =
    s2"""
       Hext to Bytes conversion specs
          to base 16 encoding $e1
    """
  import HexUtil._

  def e1 = {
    val data = "0123456789ABCDEF"
    println(s" ++++++++ ${hex2bytes(data)} __________ ")
    bytes2hex(hex2bytes(data).toOption.get, Option("-")) === "01-23-45-67-89-ab-cd-ef"
  }

}
