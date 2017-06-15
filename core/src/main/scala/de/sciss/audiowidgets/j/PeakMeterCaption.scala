/*
 *  PeakMeterCaption.java
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
package j

import java.awt.font.{FontRenderContext, GlyphVector}
import java.awt.geom.{AffineTransform, GeneralPath, Rectangle2D}
import java.awt.{BasicStroke, Color, Dimension, Font, Graphics, Graphics2D, GraphicsEnvironment, RenderingHints, Shape}
import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import javax.swing.{JComponent, SwingConstants, SwingUtilities}

import scala.annotation.switch

object PeakMeterCaption {
  private val MAJOR_TICKS       = Array(0.0f, 0.05f, 0.15f, 0.225f, 0.3f, 0.4f, 0.5f, 0.625f, 0.75f, 0.875f, 1f)
  private val LABELS            = Array("60", "50", "40", "35", "30", "25", "20", "15", "10", "5", "0")
  private val strokeMajorTicks  = new BasicStroke(1f)
  private val strokeMinorTicks  = new BasicStroke(0.5f)
  private val MAJOR_W           = 5.25f
  private val MINOR_W           = 3.5f
}

class PeakMeterCaption(orient: Int = SwingConstants.VERTICAL) extends JComponent {

  import SwingConstants._

  import PeakMeterCaption._

  private var hAlign          = RIGHT
  private var paintLabelsVar  = true

  private var recentWidth     = -1
  private var recentHeight    = -1

  private var shpMajorTicks: Shape = _
  private var shpMinorTicks: Shape = _
  private var shpLabels    : Shape = _

  private var ascentVar   = 0
  private var descentVar  = 0

  private var ticksVar    = 0

  private var vertical = orient == VERTICAL

  if (!vertical && orient != HORIZONTAL) throw new IllegalArgumentException(orient.toString)

  setPreferredSize(new Dimension(20, 20))
  setOpaque(true)
  setFont(new Font("SansSerif", Font.PLAIN, 12))
  setForeground(Color.white)
  setBackground(Color.black)
  recalcPrefSize()

  private val recalcListener = new PropertyChangeListener {
    def propertyChange(e: PropertyChangeEvent): Unit = {
      recentHeight = -1
      recalcPrefSize()
      repaint()
    }
  }

  addPropertyChangeListener("border", recalcListener)
  addPropertyChangeListener("font"  , recalcListener)

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
    recalcPrefSize()
  }

  def ticks: Int = ticksVar

  def ascent : Int = ascentVar
  def descent: Int = descentVar

  private def recalcPrefSize(): Unit = {
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
        val b = fnt.createGlyphVector(frc, LABELS(i)).getLogicalBounds
        labW  = math.max(labW, (b.getWidth + 0.5).toInt)
        labH  = math.max(labH, b.getHeight.toFloat)
        i    += 1
      }
      labW      += 2
      ascentVar  = (labH / 2).toInt
      descentVar = ascentVar
    } else {
      labW       = 0
      ascentVar  = 0
      descentVar = 0
    }

    val d = new Dimension(labW + (if (hAlign == CENTER) 12 else 5) + insets.left + insets.right,
      if (ticksVar <= 0) getPreferredSize.height else ticksVar * 2 - 1 + insets.top + insets.bottom)

    setPreferredSize(d)
    setMinimumSize(new Dimension(d.width, 2 + insets.top + insets.bottom))
    setMaximumSize(new Dimension(d.width, getMaximumSize.height))
  }

  def horizontalAlignment_=( value: Int ): Unit = if( hAlign != value ) {
    if (value != LEFT && value != RIGHT && value != CENTER)
      throw new IllegalArgumentException(value.toString)

    hAlign        = value
    recentHeight  = -1
    recalcPrefSize()
    repaint()
  }

  def horizontalAlignment: Int = hAlign

  def labelsVisible_=(b: Boolean): Unit = if (paintLabelsVar != b) {
    paintLabelsVar = b
    recentHeight   = -1
    repaint()
  }

  def labelsVisible: Boolean = paintLabelsVar

  override def paintComponent(g: Graphics): Unit = {
    val w = getWidth
    val h = getHeight
    g.setColor(getBackground)
    g.fillRect(0, 0, w, h)
    g.setColor(Color.white)

    val g2 = g.asInstanceOf[Graphics2D]
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    if ((w != recentWidth) || (h != recentHeight)) {
      recentWidth  = w
      recentHeight = h

      var xw   = 0f
      var labX = 0

      (hAlign: @switch) match {
        case LEFT =>
          labX = (MAJOR_W + 3).toInt
        case CENTER =>
          xw   = 0.5f
          labX = (MAJOR_W + 3).toInt
        case RIGHT =>
          xw = 1f
      }

      // ------------ recalculate ticks ------------

      val at		  = new AffineTransform()
			val insets	= getInsets()
			val wi		  = w - (insets.left + insets.right)
			val hi		  = h - (insets.top + insets.bottom + ascentVar + descentVar)
			val him		  = hi - 1
			val gpMajT	= new GeneralPath()
			val gpMinT	= new GeneralPath()
      at.translate(insets.left, insets.top + ascentVar)

      var j  = 0
      val jm = if (hAlign == CENTER) 2 else 1
      while (j < jm) {
        val xwEff = if (hAlign == CENTER) {
          if (j == 0) 0f else 1f
        } else xw
        val offX1 = (wi - MAJOR_W) * xwEff
        val offX2 = (wi - MINOR_W) * xwEff
        var i = 0
        while (i < MAJOR_TICKS.length) {
          val tck = MAJOR_TICKS(i)
          gpMajT.moveTo(offX1, (1f - tck) * him)
          gpMajT.lineTo(offX1 + MAJOR_W, (1f - tck) * him)
          i += 1
        }
        i = 0
        while (i < 20) {
          if ((i % 5) != 0) {
            gpMinT.moveTo(offX2, i * 0.025f * him)
            gpMinT.lineTo(offX2 + MINOR_W, i * 0.025f * him)
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
        val lbScale   = (hi - 1) * 0.004
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
            (1f - MAJOR_TICKS(i)) * 250 - gvb(i).getCenterY.toFloat), false)
          i += 1
        }
        at.setToTranslation(insets.left + labX, insets.top + ascentVar)
        at.scale(lbScale, lbScale)
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