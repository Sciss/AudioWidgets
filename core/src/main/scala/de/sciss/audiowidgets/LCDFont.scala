package de.sciss.audiowidgets

import java.awt.Font

object LCDFont {
  private lazy val _font = {
    val is = LCDFont.getClass.getResourceAsStream("Receiptional Receipt.ttf")
    require(is != null, "Font resource not found") // !!
    val res = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(13f)
    is.close()
    res
  }
  def apply(): Font = _font
}