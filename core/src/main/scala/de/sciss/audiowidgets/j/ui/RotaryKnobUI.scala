/*
 *  RotaryKnobUI.scala
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

package de.sciss.audiowidgets.j
package ui

import javax.swing.JComponent
import javax.swing.JSlider
import javax.swing.SwingUtilities
import javax.swing.UIManager
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.Stroke
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.geom.Arc2D
import java.awt.geom.Area
import java.awt.geom.GeneralPath
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.plaf.basic.BasicSliderUI

object RotaryKnobUI {
  private final val arcStartDeg     = -135f
  private final val arcExtentDeg    =  270f
  private final val arcStart        = (-arcStartDeg + 90) * math.Pi / 180
  private final val arcExtent       =   arcExtentDeg      * math.Pi / 180
  private final val argHemi         =   arcStart - math.Pi * 0.5
  private final val thumb           = new NimbusRadioThumb
  private final val strkOut: Stroke = new BasicStroke(6f)
  private final val arc: Arc2D      = new Arc2D.Float(0, 0, 10, 10, arcStartDeg, -arcExtentDeg, Arc2D.PIE)
}
class RotaryKnobUI(knob: RotaryKnob) extends BasicSliderUI(knob) {
  import RotaryKnobUI._

  private final val dashTrackHigh   = Array[Float](2f, 1f)
  private var mOver                 = false
  private var mPressed              = false
  private final val pathHand        = new GeneralPath
  private var shpHand: Shape        = null
  private final val atHand          = new AffineTransform
  private var shpHandOut: Area      = null
  private var shpTrack  : Area      = null
  private final val trackBufIn      = new Insets(0, 0, 0, 0)
  private var strkTrackHigh: Stroke = null
  private final val arcTrackHigh: Arc2D = new Arc2D.Float(0, 0, 10, 10, arcStartDeg, 0, Arc2D.OPEN)
  private final val trackCentered: PropertyChangeListener = new PropertyChangeListener {
    def propertyChange(e: PropertyChangeEvent): Unit = {
      calculateThumbLocation()
      knob.repaint()
    }
  }

  private def handColor: Color = {
    val c = NimbusHelper.mixColorWithAlpha(NimbusHelper.textColor, knob.handColor)
    if (knob.isEnabled) c else NimbusHelper.adjustColor(c, 0f, 0f, 0f, -112)
  }

  private def trackColor: Color = {
    val c = NimbusHelper.mixColorWithAlpha(NimbusHelper.controlHighlightColor, knob.trackColor)
    if (knob.isEnabled) c else NimbusHelper.adjustColor(c, 0f, 0f, 0f, -112)
  }

  private def rangeColor: Color = {
    val c = NimbusHelper.mixColorWithAlpha(NimbusHelper.baseColor, knob.rangeColor)
    if (knob.isEnabled) c else NimbusHelper.adjustColor(c, 0f, 0f, 0f, -112)
  }

  override def paintThumb(g: Graphics): Unit = {
    if (thumbRect.width == 0 || thumbRect.height == 0) return
    
    val g2 = g.asInstanceOf[Graphics2D]
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val state = (if (knob.isEnabled) NimbusHelper.STATE_ENABLED else 0) |
                (if (knob.hasFocus ) NimbusHelper.STATE_FOCUSED else 0) |
                (if (mOver         ) NimbusHelper.STATE_OVER    else 0) |
                (if (mPressed      ) NimbusHelper.STATE_PRESSED else 0)
    thumb.paint(state, knob.knobColor, g2, thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height)
    g2.setColor(handColor)
    g2.fill(shpHand)
  }

  override def paintTrack(g: Graphics): Unit = {
    val g2        = g.asInstanceOf[Graphics2D]
    val hintsOld  = g2.getRenderingHints
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING  , RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE )
    g2.setColor(trackColor)
    g2.fill(shpTrack)
    g2.setColor(rangeColor)
    val strkOrig = g2.getStroke
    g2.setStroke(strkTrackHigh)
    g2.draw(arcTrackHigh)
    g2.setStroke(strkOrig)
    g2.setRenderingHints(hintsOld)
  }

  override def paintFocus(g: Graphics): Unit = ()

  protected override def createTrackListener(knob: JSlider): BasicSliderUI#TrackListener =
    new RangeTrackListener

  protected def valueForPosition(x: Int, y: Int): Int = {
    val min = knob.getMinimum
    val max = knob.getMaximum
    val xc  = thumbRect.width  * 0.5f + thumbRect.x
    val yc  = thumbRect.height * 0.5f + thumbRect.y
    val dx  = x - xc
    val a   = math.atan2(yc - y, dx)
    val b   = math.sin(a)
    val c   = math.min(argHemi, math.acos(b))
    val d   = (argHemi - c) / argHemi * 0.5
    val v   = if (dx < 0) d else 1.0 - d
    (v * (max - min) + min + 0.5).toInt
  }

  protected override def getThumbSize: Dimension = thumbRect.getSize

  protected override def calculateThumbSize(): Unit = {
    val x = contentRect.x
    val y = contentRect.y
    val w = contentRect.width
    val h = contentRect.height
    val ext = if (knob.getPaintTrack) {
      val margin0       = math.max(1f, math.min(w, h / 0.875f) * 0.125f)
      val _ext          = (margin0 * 6).toInt
      val margin        = _ext.toFloat / 6
      val diam          = margin * 8
      val inLeft        = (w - _ext) >> 1
      val inTop         = (h - margin * 5) * 0.5f
      val inTopI        = (inTop + 0.5f).toInt
      trackBufIn.left   = inLeft
      trackBufIn.right  = w - _ext - inLeft
      trackBufIn.top    = inTopI
      trackBufIn.bottom = h - _ext - inTopI
      val xo            = x + inLeft - margin
      val yo            = y + inTop - margin
      val ring          = margin * 0.625f
      val ringH         = ring * 0.5f
      val offLow1       = ring * 0.333f
      val offLow2       = offLow1 + ring
      val extHigh       = diam - ring
      val extLow1       = diam - offLow1 - offLow1
      val extLow2       = extLow1 - ring - ring
      arc.setFrame(xo + offLow1, yo + offLow1, extLow1, extLow1)
      shpTrack = new Area(arc)
      arc.setFrame(xo + offLow2, yo + offLow2, extLow2, extLow2)
      shpTrack.subtract(new Area(arc))
      strkTrackHigh = new BasicStroke(ring, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.5f, dashTrackHigh, 0f)
      arcTrackHigh.setFrame(xo + ringH, yo + ringH, extHigh, extHigh)
      _ext

    } else {
      trackBufIn.left   = 0
      trackBufIn.top    = 0
      trackBufIn.right  = 0
      trackBufIn.bottom = 0
      math.min(w, h)
    }
    thumbRect.setSize(ext, ext)
  }

  override def getPreferredHorizontalSize: Dimension =
    if (knob.getPaintTrack) {
      new Dimension(37, 32)
    } else {
      new Dimension(27, 27)
    }

  override def getPreferredVerticalSize: Dimension = getPreferredHorizontalSize

  protected override def calculateTrackRect(): Unit = {
    trackRect.x       = contentRect.x + trackBufIn.left
    trackRect.y       = contentRect.y + trackBufIn.top
    val w             = contentRect.width  - (trackBufIn.left + trackBufIn.right )
    val h             = contentRect.height - (trackBufIn.top  + trackBufIn.bottom)
    val ext           = math.min(w, h)
    trackRect.width   = ext
    trackRect.height  = ext
    trackRect.x      += (w - ext) >> 1
    trackRect.y      += (h - ext) >> 1
  }

  protected override def calculateThumbLocation(): Unit = {
    thumbRect.x   = trackRect.x
    thumbRect.y   = trackRect.y
    val min       = knob.getMinimum
    val max       = knob.getMaximum
    val v         = (knob.getValue - min).toDouble / (max - min)
    val ext       = v * arcExtent
    val ang       = ext + arcStart
    val xc        = thumbRect.width  * 0.5f + thumbRect.x
    val yc        = thumbRect.height * 0.5f + thumbRect.y
    atHand.setToRotation(ang, xc, yc)
    pathHand.reset()
    val thumbFocusInsets = 3
    val y1        = thumbRect.y + thumbFocusInsets
    val handWidth = math.sqrt(thumbRect.width / 56.0)
    val hwh       = handWidth * 0.5
    val hwq       = handWidth * 0.25
    val y2        = (thumbRect.height - thumbFocusInsets - thumbFocusInsets) * 0.5 + handWidth + y1
    pathHand.moveTo(xc - hwq, y1)
    pathHand.lineTo(xc + hwq, y1)
    pathHand.lineTo(xc + hwh, y2)
    pathHand.lineTo(xc - hwh, y2)
    pathHand.closePath()
    shpHand = atHand.createTransformedShape(pathHand)
    shpHandOut = new Area(strkOut.createStrokedShape(shpHand))
    shpHandOut.add(new Area(shpHand))
    if (knob.centered) {
      arcTrackHigh.setAngleStart(90)
      arcTrackHigh.setAngleExtent(((0.5 - v) * arcExtent) * 180 / math.Pi)
    } else {
      arcTrackHigh.setAngleStart(arcStartDeg)
      arcTrackHigh.setAngleExtent(ext * -180 / math.Pi)
    }
  }

  protected override def installDefaults(slider: JSlider): Unit = {
    super.installDefaults(slider)
    focusInsets.left    = 0
    focusInsets.top     = 0
    focusInsets.right   = 0
    focusInsets.bottom  = 0
  }

  protected override def installListeners(slider: JSlider): Unit = {
    super.installListeners(slider)
    slider.addPropertyChangeListener("centered", trackCentered)
  }

  protected override def uninstallListeners(slider: JSlider): Unit = {
    super.uninstallListeners(slider)
    slider.removePropertyChangeListener("centered", trackCentered)
  }

  protected override def calculateTrackBuffer(): Unit =
    trackBuffer = 0

  protected override def calculateTickRect(): Unit = {
    tickRect.x        = trackRect.x
    tickRect.y        = trackRect.y
    tickRect.width    = 0
    tickRect.height   = 0
  }

  protected override def calculateLabelRect(): Unit = {
    labelRect.x       = tickRect.x
    labelRect.y       = tickRect.y
    labelRect.width   = 0
    labelRect.height  = 0
  }

  override def getPreferredSize(c: JComponent): Dimension = {
    recalculateIfInsetsChanged()
    new Dimension(getPreferredHorizontalSize)
  }

  private class RangeTrackListener extends TrackListener {
    // private var currentMouseX = 0
    // private var currentMouseY = 0
    private var mDragging     = false

    override def mousePressed(e: MouseEvent): Unit = {
      if (!knob.isEnabled) return
      currentMouseX = e.getX
      currentMouseY = e.getY
      if (knob.isRequestFocusEnabled) knob.requestFocus()

      if (shpHandOut.contains(currentMouseX, currentMouseY) &&
        UIManager.getBoolean("Slider.onlyLeftMouseButtonDrag") && !SwingUtilities.isLeftMouseButton(e)) {
          return
        }

      if (!SwingUtilities.isLeftMouseButton(e)) return

      mPressed  = true
      mDragging = true
      knob.setValueIsAdjusting(true)
      knob.setValue(valueForPosition(e.getX, e.getY))
      knob.repaint()
    }

    override def mouseDragged(e: MouseEvent): Unit =
      if (mDragging) knob.setValue(valueForPosition(e.getX, e.getY))

    override def mouseReleased(e: MouseEvent): Unit =
      if (mPressed) {
        mPressed = false
        mDragging = false
        knob.setValueIsAdjusting(false)
        knob.repaint()
      }

    override def mouseEntered(e: MouseEvent): Unit = {
      if (!knob.isEnabled) return
      mOver = true
      knob.repaint()
    }

    override def mouseExited(e: MouseEvent): Unit =
      if (mOver) {
        mOver = false
        knob.repaint()
      }
  }
}