/*
 *  PeakMeterCaption
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2020 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets
package j

import java.awt.font.{FontRenderContext, GlyphVector}
import java.awt.geom.{AffineTransform, GeneralPath, Rectangle2D}
import java.awt.{BasicStroke, Color, Dimension, Font, Graphics, Graphics2D, GraphicsEnvironment, RenderingHints, Shape}
import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import javax.swing.{JComponent, SwingConstants, SwingUtilities}

import scala.annotation.switch

object PeakMeterCaption {
  private final val MAJOR_TICKS       = Array(0.0f, 0.05f, 0.15f, 0.225f, 0.3f, 0.4f, 0.5f, 0.625f, 0.75f, 0.875f, 1f)
  private final val LABELS            = Array("60", "50", "40", "35", "30", "25", "20", "15", "10", "5", "0")
  private final val strokeMajorTicks  = new BasicStroke(1f)
  private final val strokeMinorTicks  = new BasicStroke(0.5f)
  private final val MAJOR_EXT         = 5.25f
  private final val MINOR_EXT         = 3.5f
  private final val PIH               = Math.PI/2
}

class PeakMeterCaption(orient: Int = SwingConstants.VERTICAL) extends JComponent {

  import SwingConstants._

  import PeakMeterCaption._

  private[this] var hAlign          = RIGHT
  private[this] var paintLabelsVar  = true

  private[this] var recentWidth     = -1
  private[this] var recentHeight    = -1

  private[this] var shpMajorTicks: Shape = _
  private[this] var shpMinorTicks: Shape = _
  private[this] var shpLabels    : Shape = _

  private[this] var ascentVar   = 0
  private[this] var descentVar  = 0

  private[this] var ticksVar    = 0

  private[this] var vertical = orient == VERTICAL

  if (!vertical && orient != HORIZONTAL) throw new IllegalArgumentException(orient.toString)

  setPreferredSize(new Dimension(20, 20))
  setOpaque(true)
//  setFont(new Font("SansSerif", Font.PLAIN, 12))
  setFont({
    val f0 = new Font("SansSerif", Font.PLAIN, 1)
    f0.deriveFont(9.6f)
  })
  setForeground(Color.white)
  setBackground(Color.black)
  recalculatePreferredSize()

  private[this] val recalculationListener = new PropertyChangeListener {
    def propertyChange(e: PropertyChangeEvent): Unit = {
      recentHeight = -1  // triggers update in `paintComponent`
      recalculatePreferredSize()
      repaint()
    }
  }

  addPropertyChangeListener("border", recalculationListener)
  addPropertyChangeListener("font"  , recalculationListener)

  def orientation_=(orient: Int): Unit = {
    val newVertical = orient == VERTICAL
    if (!newVertical && orient != HORIZONTAL) {
      throw new IllegalArgumentException(orient.toString)
    }
    if (newVertical != vertical) {
      vertical = newVertical
      // XXX
    }
  }

  def orientation: Int = if (vertical) VERTICAL else HORIZONTAL

  def ticks_=(num: Int): Unit = if (ticksVar != num) {
    ticksVar = num
    recalculatePreferredSize()
  }

  def ticks: Int = ticksVar

  def ascent : Int = ascentVar
  def descent: Int = descentVar

  private def recalculatePreferredSize(): Unit = {
    val insets = getInsets()
    var labW = 0

    if (paintLabelsVar) {
      val fnt = getFont
      val w   = SwingUtilities.getWindowAncestor(this)

      val gc = if (w != null) {
        w.getGraphicsConfiguration
      } else {
        GraphicsEnvironment.getLocalGraphicsEnvironment.
          getDefaultScreenDevice.getDefaultConfiguration
      }
      val frc  = new FontRenderContext(gc.getNormalizingTransform, true, true)
      var labH = 0f
      var i    = 0
      while (i < LABELS.length) {
        val b = fnt.createGlyphVector(frc, LABELS(i)).getLogicalBounds // getVisualBounds // getLogicalBounds
        labW  = math.max(labW, (b.getWidth + 0.5).toInt)
        labH  = math.max(labH, b.getHeight.toFloat)
        i    += 1
      }
      labW      += 5
//      println(s"labW $labW")
      val labHi  = (labH + 0.5f).toInt
      ascentVar  = labHi / 2 // + 2 // (labH / 2).toInt
      descentVar = labHi - ascentVar // ascentVar
//      ascentVar  = (labH / 2).toInt
//      descentVar = ascentVar
    } else {
      labW       = 0
      ascentVar  = 0
      descentVar = 0
    }

    val iw = if (vertical) insets.left + insets.right else insets.top + insets.bottom
    val ih = if (vertical) insets.top + insets.bottom else insets.left + insets.right
    val pw = labW + (if (hAlign == CENTER) 12 else 5) + iw
    val ph = if (ticksVar <= 0) {
      val pref = getPreferredSize
      if (vertical) pref.height else pref.width
    } else ticksVar * 2 - 1 + ih

    val max = getMaximumSize

    if (vertical) {
      setPreferredSize(new Dimension(pw, ph))
      setMinimumSize  (new Dimension(pw, 2 + ih))
      setMaximumSize  (new Dimension(pw, max.height))
    } else {
      setPreferredSize(new Dimension(ph, pw))
      setMinimumSize  (new Dimension(2 + ih, pw))
      setMaximumSize  (new Dimension(max.width, pw))
    }
  }

  def horizontalAlignment_=(value: Int): Unit = if (hAlign != value) {
    if (value != LEFT && value != RIGHT && value != CENTER)
      throw new IllegalArgumentException(value.toString)

    hAlign        = value
    recentHeight  = -1  // triggers update in `paintComponent`
    recalculatePreferredSize()
    repaint()
  }

  def horizontalAlignment: Int = hAlign

  def labelsVisible_=(b: Boolean): Unit = if (paintLabelsVar != b) {
    paintLabelsVar = b
    recentHeight   = -1  // triggers update in `paintComponent`
    repaint()
  }

  def labelsVisible: Boolean = paintLabelsVar

  override def paintComponent(g: Graphics): Unit = {
    val w = getWidth
    val h = getHeight
    g.setColor(getBackground)
//    g.setColor(Color.red)
    g.fillRect(0, 0, w, h)
    g.setColor(Color.white)

    val g2 = g.asInstanceOf[Graphics2D]
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    if ((w != recentWidth) || (h != recentHeight)) {
      recentWidth  = w
      recentHeight = h

      var xw      = 0f
      var labOff  = 0

      (hAlign: @switch) match {
        case LEFT =>
          labOff = (MAJOR_EXT + 3).toInt
        case CENTER =>
          xw = 0.5f
          labOff = (MAJOR_EXT + 3).toInt
        case RIGHT =>
          xw = 1f
      }

      // ------------ recalculate ticks ------------

      val at		  = new AffineTransform()
      val insets	= getInsets()
      val wi		  = if (vertical) w - (insets.left + insets.right)
                    else          h - (insets.top + insets.bottom)
      val hi		  = if (vertical) h - (insets.top + insets.bottom + ascentVar + descentVar)
                    else          w - (insets.left + insets.right + ascentVar + descentVar)
      val him		  = hi - 1
      val gpMajT	= new GeneralPath()
      val gpMinT	= new GeneralPath()

      if (vertical) {
        at.translate(insets.left, insets.top + ascentVar)
      } else {
//        println(s"insets = T ${insets.top} L ${insets.left} B ${insets.bottom} R ${insets.right}, ascent $ascentVar, descent $descentVar, labOff $labOff")
        at.translate(insets.left + w - (ascentVar + 1), insets.top)
        at.rotate(PIH)
      }

      var j  = 0
      val jm = if (hAlign == CENTER) 2 else 1
      while (j < jm) {
        val xwEff = if (hAlign == CENTER) {
          if (j == 0) 0f else 1f
        } else xw
        val offX1 = (wi - MAJOR_EXT) * xwEff
        val offX2 = (wi - MINOR_EXT) * xwEff
        var i = 0
        while (i < MAJOR_TICKS.length) {
          val tck = MAJOR_TICKS(i)
          gpMajT.moveTo(offX1, (1f - tck) * him)
          gpMajT.lineTo(offX1 + MAJOR_EXT, (1f - tck) * him)
          i += 1
        }
        i = 0
        while (i < 20) {
          if ((i % 5) != 0) {
            gpMinT.moveTo(offX2, i * 0.025f * him)
            gpMinT.lineTo(offX2 + MINOR_EXT, i * 0.025f * him)
          }
          i += 1
        }
        j += 1
      }
      shpMajorTicks = at.createTransformedShape(gpMajT)
      shpMinorTicks = at.createTransformedShape(gpMinT)

      // ------------ recalculate labels ------------
      if (paintLabelsVar) {
        val frc       = g2.getFontRenderContext
        val gp        = new GeneralPath()
//        println(s"vertical $vertical, hi $hi")
//        val lbScale   = (hi - 1) * 0.004
        val lbScale = hi - 1
//        println(s"vertical $vertical, lbScale $lbScale")
        val numLabels = LABELS.length
        val gv        = new Array[GlyphVector](numLabels)
        val gvb       = new Array[Rectangle2D](numLabels)
        val fnt       = getFont
        var maxWidth  = 0f
        var i = 0
        while (i < numLabels) {
          gv(i)    = fnt.createGlyphVector(frc, LABELS(i))
          val b    = gv(i).getLogicalBounds
          gvb(i)   = b
          maxWidth = math.max(maxWidth, b.getWidth.toFloat)
          i       += 1
        }
        i = 0
        while (i < gv.length) {
          gp.append(gv(i).getOutline(
            (maxWidth - gvb(i).getWidth.toFloat) * xw + 1.5f,
            (1f - MAJOR_TICKS(i)) * lbScale /* 250 */ - gvb(i).getCenterY.toFloat), false)
          i += 1
        }

        at.setToIdentity()
        if (vertical) {
          at.translate(insets.left + labOff, insets.top + ascentVar)
        } else {
          at.translate(insets.left + w - ascentVar, insets.top + labOff)
          at.rotate(PIH)
        }
        shpLabels = at.createTransformedShape(gp)
      }
    }
    g2.setStroke(strokeMajorTicks)
    g2.draw(shpMajorTicks)
    g2.setStroke(strokeMinorTicks)
    g2.draw(shpMinorTicks)
    if (paintLabelsVar) g2.fill(shpLabels)
  }
}