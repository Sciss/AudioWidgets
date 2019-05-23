/*
 *  RecessedBorder
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

package de.sciss.audiowidgets.j

import java.awt.geom.{Area, Rectangle2D, RoundRectangle2D}
import java.awt.{Color, Component, Graphics, Graphics2D, Insets, RenderingHints, Shape}
import javax.swing.border.AbstractBorder

object RecessedBorder {
  private final val diameter = 6
  private final val insets   = new Insets(3, 3, 4, 4)
}

class RecessedBorder(c: Color = Color.black)
  extends AbstractBorder {

  import RecessedBorder._

  private final var colorVar = c
  private final var shpBg     : Shape = _

  private final var recentWidth  = -1
  private final var recentHeight = -1

  def color_=(value: Color): Unit = colorVar = value

  def color: Color = colorVar

  override def getBorderInsets(c: Component) = new Insets(insets.top, insets.left, insets.bottom, insets.right)

  override def getBorderInsets(c: Component, i: Insets): Insets = {
    i.top    = insets.top
    i.left   = insets.left
    i.bottom = insets.bottom
    i.right  = insets.right
    i
  }

  override def paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int): Unit = {
    val g2      = g.asInstanceOf[Graphics2D]
    val atOrig  = g2.getTransform

    g2.translate(x, y)

    if ((width != recentWidth) || (height != recentHeight)) {
      val r = new RoundRectangle2D.Float(0.0f, 0.0f, width, height, diameter, diameter)
      val a = new Area(r)
      a.subtract(new Area(new Rectangle2D.Float(
        insets.left,
        insets.top,
        width  - insets.left - insets.right,
        height - insets.top  - insets.bottom)))
      shpBg        = a

      recentWidth  = width
      recentHeight = height
    }

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setPaint(colorVar)
    g2.fill(shpBg)

    g2.setTransform(atOrig)
  }
}