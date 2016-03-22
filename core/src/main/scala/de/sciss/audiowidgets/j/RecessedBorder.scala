/*
 *  RecessedBorder.java
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

package de.sciss.audiowidgets.j

import javax.swing.border.AbstractBorder
import java.awt.{RenderingHints, BasicStroke, Color, Component, Graphics, Graphics2D, Insets, Shape}
import java.awt.geom.{Rectangle2D, Area, RoundRectangle2D}

object RecessedBorder {
  private final val diameter      = 4
  private final val colorDark     = new Color(0x00, 0x00, 0x00, 0x88)
  private final val colorLight    = new Color(0xFF, 0xFF, 0xFF, 0xD8)
  private final val strokeOutline = new BasicStroke(1.0f)
  private final val strokeInline  = new BasicStroke(2.0f)
  private final val insets        = new Insets(3, 3, 4, 4)
}

class RecessedBorder(c: Color = Color.black)
  extends AbstractBorder {

  import RecessedBorder._

  private final var colorVar = c
  private final var shpBg     : Shape = null
  private final var shpInline : Shape = null
  private final var shpOutline: Shape = null

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
      val r        = new RoundRectangle2D.Float(1.0f, 0.5f, width - 2.0f, height - 1.5f, diameter, diameter)
      val r2       = new RoundRectangle2D.Float(0.5f, 0.0f, width - 1.5f, height - 1.0f, diameter, diameter)
      val a        = new Area(r)
      a.subtract(new Area(new Rectangle2D.Float(
        insets.left,
        insets.top,
        width - insets.left - insets.right,
        height - insets.top - insets.bottom)))

      shpOutline   = strokeOutline.createStrokedShape(r2)
      shpInline    = strokeInline .createStrokedShape(r2)
      shpBg        = a

      recentWidth  = width
      recentHeight = height
    }

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setPaint(colorDark)
    g2.fill(shpOutline)
    g2.translate(1, 1)
    g2.setPaint(colorLight)
    g2.fill(shpInline)
    g2.translate(-1, -1)
    g2.setPaint(colorVar)
    g2.fill(shpBg)

    g2.setTransform(atOrig)
  }
}