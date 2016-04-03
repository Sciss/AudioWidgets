/*
 *  LCDColors.scala
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

import java.awt.Color

object LCDColors {
  def background: Color = if (Util.isDarkSkin) blackBg else defaultBg
  def foreground: Color = if (Util.isDarkSkin) blackFg else defaultFg

  final val defaultFg = Color.darkGray
  final val defaultBg = Color.getHSBColor(0.194f, 0.190f, 0.805f)
  final val blueFg    = new Color(205, 232, 254)
  final val blueBg    = new Color(15, 42, 64)
  final val grayFg    = Color.darkGray
  final val grayBg    = Color.lightGray
  final val redFg     = new Color(60, 30, 20)
  final val redBg     = new Color(200, 100, 100)
  final val blackFg   = new Color(0xE0, 0xE0, 0xE0)
  final val blackBg   = new Color(0x10, 0x10, 0x10)
}