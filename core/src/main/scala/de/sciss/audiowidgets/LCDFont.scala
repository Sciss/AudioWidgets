package de.sciss.audiowidgets

import java.awt.Font
import java.awt.geom.AffineTransform

object LCDFont {
  private lazy val _font = {
    val is = LCDFont.getClass.getResourceAsStream("Receiptional Receipt.ttf")
    require(is != null, "Font resource not found") // !!
    val res = Font.createFont(Font.TRUETYPE_FONT, is)
    is.close()
    res.deriveFont(11f)
       .deriveFont(Font.PLAIN, AffineTransform.getTranslateInstance(0,4.0)) // problem with ascent
  }
  def apply(): Font = _font
}