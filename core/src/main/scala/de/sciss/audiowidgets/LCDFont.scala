/*
 *  LCDFont.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2016 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets

import java.awt.Font
import java.awt.geom.AffineTransform

object LCDFont {
  //  private lazy val _fontOLD = {
  //    val is = LCDFont.getClass.getResourceAsStream("Receiptional Receipt.ttf")
  //    require(is != null, "Font resource not found") // !!
  //    val res = Font.createFont(Font.TRUETYPE_FONT, is)
  //    is.close()
  //    res.deriveFont(11f)
  //      .deriveFont(Font.PLAIN, AffineTransform.getTranslateInstance(0,4.0)) // problem with ascent
  //  }

  private lazy val _font = {
    val is = LCDFont.getClass.getResourceAsStream("FamiliadaMono.ttf")
    if (is == null) sys.error("Font resource not found") // !!
    val res = Font.createFont(Font.TRUETYPE_FONT, is)
    is.close()
    res.deriveFont(11.5f)
  }

  /** Returns a monospaced "LCD" style font, at size 11.5pt. Other sizes may be derived using `.deriveFont`. */
  def apply(): Font = _font
}