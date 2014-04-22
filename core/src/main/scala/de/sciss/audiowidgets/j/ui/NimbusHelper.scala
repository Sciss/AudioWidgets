/*
 *  NimbusHelper.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2014 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets.j.ui

import javax.swing.UIDefaults
import javax.swing.UIManager
import java.awt.Color

// straight from Java (SwingOSC)...
object NimbusHelper {
  def focusColor: Color = {
    val c = if (nimbusDefaults == null) null else nimbusDefaults.getColor("nimbusFocus")
    if (c == null) defaultFocusColor else c
  }

  def baseColor: Color = {
    val c = if (nimbusDefaults == null) null else nimbusDefaults.getColor("nimbusBase")
    if (c == null) defaultBaseColor else c
  }

  def textColor: Color = {
    val c = if (nimbusDefaults == null) null else nimbusDefaults.getColor("text")
    if (c == null) defaultTextColor else c
  }

  def selectedTextColor: Color = {
    val c = if (nimbusDefaults == null) null else nimbusDefaults.getColor("selectedText")
    if (c == null) defaultSelectedTextColor else c
  }

  def controlHighlightColor: Color = {
    val c = if (nimbusDefaults == null) null else nimbusDefaults.getColor("controlHighlight")
    if (c == null) defaultControlHighlightColor else c
  }

  def selectionBackgroundColor: Color = {
    val c = if (nimbusDefaults == null) null else nimbusDefaults.getColor("nimbusSelectionBackground")
    if (c == null) defaultSelectionBackgroundColor else c
  }

  def blueGreyColor(base: Color): Color = {
    val c = if (nimbusDefaults == null) null else nimbusDefaults.getColor("nimbusBlueGrey")
    if (c == null) defaultBlueGreyColor(base) else c
  }

  private def defaultBlueGreyColor(base: Color): Color =
    adjustColor(base, 0.032459438f, -0.52518797f, 0.19607842f, 0)
  
  def adjustColor(c: Color, hueOffset: Float, satOffset: Float, briOffset: Float, alphaOffset: Int): Color = {
    val sameColor = hueOffset == 0f && satOffset == 0f && briOffset == 0f
    val sameAlpha = alphaOffset == 0
    if (sameColor) {
      if (sameAlpha) return c
      val cAlpha = c.getAlpha
      return new Color(c.getRed, c.getGreen, c.getBlue, math.max(0, math.min(0xFF, cAlpha + alphaOffset)))
    }
    Color.RGBtoHSB(c.getRed, c.getGreen, c.getBlue, hsbArr)
    val hue     = hsbArr(0) + hueOffset
    val sat     = math.max(0f, math.min(1f, hsbArr(1) + satOffset))
    val bri     = math.max(0f, math.min(1f, hsbArr(2) + briOffset))
    val rgb     = Color.HSBtoRGB(hue, sat, bri)
    val cAlpha  = c.getAlpha
    val a       = if (sameAlpha) cAlpha else math.max(0, math.min(0xFF, cAlpha + alphaOffset))
    val rgba    = (rgb & 0xFFFFFF) | (a << 24)
    new Color(rgba, true)
  }

  def mixColorWithAlpha(base: Color, mix: Color): Color = {
    if (mix == null) return base
    val a0 = mix.getAlpha
    if (a0 == 0) {
      return base
    }
    else if (a0 == 0xFF) return mix
    val wm  = a0.toFloat / 0xFF
    val wb  = 1f - wm
    val r   = (base.getRed   * wb + mix.getRed   * wm + 0.5f).toInt
    val g   = (base.getGreen * wb + mix.getGreen * wm + 0.5f).toInt
    val b   = (base.getBlue  * wb + mix.getBlue  * wm + 0.5f).toInt
    new Color(r, g, b)
  }

  final val STATE_ENABLED: Int = 0x01
  final val STATE_OVER   : Int = 0x02
  final val STATE_FOCUSED: Int = 0x04
  final val STATE_PRESSED: Int = 0x08

  private lazy val nimbusDefaults: UIDefaults = {
    val current = UIManager.getLookAndFeel
    if (current.getName.toLowerCase == "nimbus") current.getDefaults else null
  }

  private final val defaultFocusColor               : Color = new Color(115, 164, 209, 255) // new Color(162, 162, 162, 255)
  private final val defaultSelectionBackgroundColor : Color = new Color(57, 105, 138, 255)

  // original Nimbus
  private final val defaultBaseColor                : Color = new Color(51, 98, 140, 255)
  private final val defaultControlHighlightColor    : Color = new Color(233, 236, 242, 255)

  // desaturated to work better with generic LaFs
  //  private final val defaultBaseColor                : Color = new Color(95, 95, 95, 255)
  //  private final val defaultControlHighlightColor    : Color = new Color(237, 237, 237, 255)

  private final val defaultTextColor                : Color = Color.black
  private final val defaultSelectedTextColor        : Color = Color.white
  private final val hsbArr                                  = new Array[Float](3)
}
