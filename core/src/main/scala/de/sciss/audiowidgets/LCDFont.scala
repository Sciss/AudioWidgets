/*
 *  LCDFont.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2019 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets

import java.awt.Font

import scala.util.control.NonFatal

object LCDFont {
  private[this] def fallBackFont = new Font(Font.MONOSPACED, Font.PLAIN, 10)

  private lazy val _font = {
    val url = LCDFont.getClass.getResource("FamiliadaMono.ttf")
    if (url == null) {
      Console.err.println("LCDFont: resource not found")
      fallBackFont
    } else try {
      val is = url.openStream()
      try {
        val res = Font.createFont(Font.TRUETYPE_FONT, is)
        res.deriveFont(11.5f)
      } finally {
        is.close()
      }
    } catch {
      case NonFatal(e) =>
        Console.err.println("LCDFont: Cannot create font")
        e.printStackTrace()
        fallBackFont
    }
  }

  /** Returns a monospaced "LCD" style font, at size 11.5pt. Other sizes may be derived using `.deriveFont`. */
  def apply(): Font = _font
}