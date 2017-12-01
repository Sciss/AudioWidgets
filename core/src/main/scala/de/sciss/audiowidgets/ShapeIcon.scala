/*
 *  ShapeIcon.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2017 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets

import java.awt.{Component, Graphics, Graphics2D, Paint, RenderingHints, Shape}
import javax.swing.Icon

final class ShapeIcon(shape: Shape, paint: Paint, shadow: Paint, width: Int, height: Int) extends Icon {
  def paintIcon(c: Component, g: Graphics, x: Int, y: Int): Unit = {
    val g2: Graphics2D = g.asInstanceOf[Graphics2D]
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING  , RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE )
    g2.translate(x, y)
    g2.setPaint(shadow)
    g2.draw(shape)
    g2.setPaint(paint)
    g2.fill(shape)
    g2.translate(-x, -y)
  }

  def getIconWidth : Int = width
  def getIconHeight: Int = height
}
