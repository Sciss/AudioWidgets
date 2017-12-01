/*
 *  DualRangeSliderUI.java
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
package j
package ui

import java.awt.event.{FocusEvent, FocusListener, KeyEvent, KeyListener, MouseEvent, MouseListener, MouseMotionListener}
import java.awt.geom.GeneralPath
import java.awt.{Color, Dimension, GradientPaint, Graphics, Graphics2D, Insets, Paint, Rectangle, RenderingHints, Shape}
import javax.swing.event.{ChangeEvent, ChangeListener}
import javax.swing.plaf.ComponentUI
import javax.swing.{JComponent, SwingConstants, SwingUtilities, UIManager}

import scala.annotation.switch

// XXX TODO: only handles horizontal orientation at the moment
// XXX TODO: should have custom handle colour
// XXX TODO: extentFixed not honoured
object DualRangeSliderUI {
  private final val colrFillTrackLight      = new Color(0x00, 0x00, 0x00, 0x26)
  private final val colrDrawTrackLight      = new Color(0x00, 0x00, 0x00, 0x4C)
  private final val colrFillSelLight        = new Color(0x00, 0x00, 0x00, 0x3B) // 0x59
  private final val colrDrawHandleLight     = new Color(0x73, 0x76, 0x68) // XXX TODO: depends on user colour
  private final val colrDrawHandleSelLight  = new Color(0x33, 0x33, 0x33)

  private final val colrFillTrackDark       = new Color(0x00, 0x00, 0x00, 0x26)
  private final val colrDrawTrackDark       = new Color(0xFF, 0xFF, 0xFF, 0x26)
  private final val colrFillSelDark         = new Color(0xFF, 0xFF, 0xFF, 0x59)
  private final val colrDrawHandleDark      = new Color(32, 32, 32)
  private final val colrDrawHandleSelDark   = new Color(48, 77, 130)

  // private final var colrDrawValue   = new Color(0x00, 0x00, 0x00, 0x73)
  // HSB thumb outline un-selected: 73, 12, 46
  // HSB thumb fill gradient: low = 72, 12, 87 ; high = 69, 9, 90
  // reference color: 0xCC, 0xD2, B0, or HSB 71, 16, 82
  // RGB thumb fill gradient selected: high = 0x6A, 0x6A, 0x6A (0x6D); low = 0x4D, 0x4D, 0x4D (0x41)

  // sizes: thumb height = 8; insets: top 4; bottom 5; left and right 8

  private final val trackRect = new Rectangle
  private final var scale     = 0.0
  private final var xVal      = 0
  private final var xLo       = 0
  private final var xHi       = 0

  private final val shpValue: Shape = {
    val gp = new GeneralPath()
    gp.moveTo( 0.5f ,  2.5f)
    gp.lineTo(-4.25f, -4.5f)
    gp.lineTo( 5.25f, -4.5f)
    gp.closePath()
    gp
  }

  private final val shpLow: Shape = {
    val gp = new GeneralPath()
    gp.moveTo( 0.5f ,  4.5f)
    gp.lineTo( 0.5f , 12.5f)
    gp.lineTo(-7.5f , 12.5f)
    gp.lineTo(-7.5f ,  9.5f)
    gp.lineTo(-4.25f,  9.5f)
    gp.closePath()
    gp
  }

  private final val shpHigh: Shape = {
    val gp = new GeneralPath()
    gp.moveTo( 0.5f ,  4.5f)
    gp.lineTo( 0.5f , 12.5f)
    gp.lineTo( 8.5f , 12.5f)
    gp.lineTo( 8.5f ,  9.5f)
    gp.lineTo( 5.25f,  9.5f)
    gp.closePath()
    gp
  }

  private final val pntFillValueLight: Paint = new GradientPaint(
    0f, 1f, new Color(0xDD, 0xE1, 0xC9),
    0f, 4f, new Color(0xD9, 0xDE, 0xC4))

  private final val pntFillValueSelLight: Paint = new GradientPaint(
    0f, 1f, new Color(0x6A, 0x6A, 0x6A),
    0f, 4f, new Color(0x4D, 0x4D, 0x4D))

  private final val pntFillRangeLight: Paint = new GradientPaint(
    0f, 11f, new Color(0xE2, 0xE5, 0xD1),
    0f, 15f, new Color(0xD9, 0xDE, 0xC4))

  private final val pntFillRangeSelLight: Paint = new GradientPaint(
    0f, 11f, new Color(0x6D, 0x6D, 0x6D),
    0f, 15f, new Color(0x41, 0x41, 0x41))

  private final val pntFillValueDark: Paint = new GradientPaint(
    0f, 1f, new Color(0x6A, 0x6A, 0x6A),
    0f, 4f, new Color(0x4D, 0x4D, 0x4D))

  private final val pntFillValueSelDark: Paint = new GradientPaint(
    0f, 1f, new Color(0x5E, 0x97, 0xFF),
    0f, 4f, new Color(0xC4, 0xC4, 0xC4))

  private final val pntFillRangeDark: Paint = new GradientPaint(
    0f, 11f, new Color(0x6D, 0x6D, 0x6D),
    0f, 15f, new Color(0x41, 0x41, 0x41))

  private final val pntFillRangeSelDark: Paint = new GradientPaint(
    0f, 11f, new Color(0x5E, 0x97, 0xFF),
    0f, 15f, new Color(0xC4, 0xC4, 0xC4))

  private sealed trait MaybeHandle {
    def valueOption(m: DualRangeModel): Option[Int]
  }
  private case object NoHandle extends MaybeHandle {
    def valueOption(m: DualRangeModel): Option[Int] = None
  }
  private sealed trait Handle extends MaybeHandle {
    def value(m: DualRangeModel): Int
    def valueOption(m: DualRangeModel): Option[Int] = Some(value(m))
  }
  private case object ValueHandle extends Handle {
    def value(m: DualRangeModel): Int = m.value
  }
  private case object LowHandle extends Handle {
    def value(m: DualRangeModel): Int = m.range._1
  }
  private case object HighHandle extends Handle {
    def value(m: DualRangeModel): Int = m.range._2
  }

  private final val ValueHandleRadius = 5
  private final val RangeHandleWidth  = 9
}
class DualRangeSliderUI(slider: DualRangeSlider) extends ComponentUI {
  import DualRangeSliderUI._

  private[this] var focusHandle: MaybeHandle = NoHandle
  private[this] var _insets: Insets = _

  private[this] var dragHandle: MaybeHandle = NoHandle
  
  private[this] var colrFillTrack     : Color = _
  private[this] var colrDrawTrack     : Color = _
  private[this] var colrFillSel       : Color = _
  private[this] var colrDrawHandle    : Color = _
  private[this] var colrDrawHandleSel : Color = _
  private[this] var pntFillValue      : Paint = _
  private[this] var pntFillValueSel   : Paint = _
  private[this] var pntFillRange      : Paint = _
  private[this] var pntFillRangeSel   : Paint = _

  override def installUI(c: JComponent): Unit = {
    dragHandle = NoHandle
    installListeners()
    _insets = slider.getInsets
    val isDark = Util.isDarkSkin
    if (isDark) {
      colrFillTrack     = colrFillTrackDark
      colrDrawTrack     = colrDrawTrackDark
      colrFillSel       = colrFillSelDark
      colrDrawHandle    = colrDrawHandleDark
      colrDrawHandleSel = colrDrawHandleSelDark
      pntFillValue      = pntFillValueDark
      pntFillValueSel   = pntFillValueSelDark
      pntFillRange      = pntFillRangeDark
      pntFillRangeSel   = pntFillRangeSelDark
    } else {
      colrFillTrack     = colrFillTrackLight
      colrDrawTrack     = colrDrawTrackLight
      colrFillSel       = colrFillSelLight
      colrDrawHandle    = colrDrawHandleLight
      colrDrawHandleSel = colrDrawHandleSelLight
      pntFillValue      = pntFillValueLight
      pntFillValueSel   = pntFillValueSelLight
      pntFillRange      = pntFillRangeLight
      pntFillRangeSel   = pntFillRangeSelLight
    }
  }

  override def uninstallUI(c: JComponent): Unit = {
    // uninstallDefaults()
    uninstallListeners()
    // uninstallKeyboardActions()
  }

  private def calcGeometry(): Unit = {
    val cw    = slider.getWidth  - (_insets.left + _insets.right )
    val ch    = slider.getHeight - (_insets.top  + _insets.bottom)
    val m     = slider.model
    val yOff  = (ch - 17) >> 1
    // println(yOff)
    trackRect.setBounds(8 + _insets.left, yOff + 4 + _insets.top, cw - 16, 8)
    scale     = (trackRect.width - 1).toDouble / (m.maximum - m.minimum)
    xVal      = ((m.value - m.minimum) * scale + 0.5).toInt + trackRect.x
    val r     = m.range
    xLo       = ((r._1    - m.minimum) * scale + 0.5).toInt + trackRect.x
    xHi       = ((r._2    - m.minimum) * scale + 0.5).toInt + trackRect.x
  }

  private def screenToValue(x: Int): Int = {
    val m   = slider.model
    val ext = m.maximum - m.minimum
    val sc  = ext.toDouble / (trackRect.width - 1)
    math.max(0, math.min(ext, ((x - trackRect.x) * sc + 0.5).toInt)) + m.minimum
  }

  private object trackListener
    extends MouseListener with MouseMotionListener with FocusListener with KeyListener with ChangeListener {

    def stateChanged(e: ChangeEvent): Unit = {
      // println("Change!")
      slider.repaint()
    }

    def mousePressed (e: MouseEvent): Unit = {
      if (!slider.isEnabled) return

      val mx = e.getX
      val my = e.getY

      if (slider.isRequestFocusEnabled) slider.requestFocus()
      if (!SwingUtilities.isLeftMouseButton(e)) return

      val m = slider.model
      calcGeometry()

      val ty2 = trackRect.y + trackRect.height

      val valueOk = slider.valueVisible && slider.valueEditable
      if (valueOk) {
        if (my < ty2) {
          val hit = mx >= xVal - ValueHandleRadius && mx <= xVal + ValueHandleRadius
          if (hit) {
            focusHandle = ValueHandle
          } else {
            val v = screenToValue(mx)
            m.setRangeProperties(value = v, adjusting = true)
          }
          dragHandle = ValueHandle
          slider.repaint()
        }
      }

      if (slider.rangeVisible && slider.rangeEditable) {
        if (my >= ty2) {
          val hitLo = mx > xLo - RangeHandleWidth && mx <= xLo
          val hitHi = mx < xHi + RangeHandleWidth && mx >= xHi
          if (hitLo) {
            focusHandle = LowHandle
          } else if (hitHi) {
            focusHandle = HighHandle
          }
          val dlo   = math.abs(mx - xLo)
          val dhi   = math.abs(mx - xHi)
          val isLo  = dlo < dhi || (dlo == dhi && mx < xLo)
          if (!hitLo && !hitHi) {
            val r = m.range
            val v = screenToValue(mx)
            val newV  = if (valueOk) v else m.value
            val rNew = if (isLo)
              r.copy(_1 = v)
            else
              r.copy(_2 = v)
            m.setRangeProperties(value = newV, range = rNew, adjusting = true)
          }
          dragHandle = if (isLo) LowHandle else HighHandle
          slider.repaint()
        }
      }
    }

    def mouseReleased(e: MouseEvent): Unit =
      if (dragHandle != NoHandle) {
        dragHandle = NoHandle
        slider.model.adjusting = false
      }

    def mouseDragged(e: MouseEvent): Unit =
      if (dragHandle != NoHandle) {
        calcGeometry()
        val mx    = e.getX
        val v     = screenToValue(mx)
        adjustValue(dragHandle, v, a = true)
      }

    private def adjustValue(h: MaybeHandle, v: Int, a: Boolean): Unit = {
      val m = slider.model
      if (slider.valueVisible && slider.valueEditable)
        m.setRangeProperties(value = v, adjusting = a)
      if (slider.rangeVisible && slider.rangeEditable) {
        if (h == LowHandle)
          m.setRangeProperties(range = (v, math.max(v, m.range._2)), adjusting = a)
        else if (h == HighHandle)
          m.setRangeProperties(range = (math.min(v, m.range._1), v), adjusting = a)
      }
    }

    def mouseMoved   (e: MouseEvent): Unit = ()
    def mouseClicked (e: MouseEvent): Unit = ()
    def mouseEntered (e: MouseEvent): Unit = ()
    def mouseExited  (e: MouseEvent): Unit = ()

    def focusGained(e: FocusEvent): Unit = slider.repaint()
    def focusLost  (e: FocusEvent): Unit = slider.repaint()

    private def incValue(amt: Int): Unit =
      focusHandle.valueOption(slider.model).foreach { v =>
        adjustValue(focusHandle, v + amt, a = false)
      }

    private def expandRange(): Unit =
      if (slider.valueVisible && slider.rangeVisible && slider.rangeEditable) {
        val m = slider.model
        val v = m.value
        val r = m.range
        m.range = if (math.abs(v - r._1) < math.abs(v - r._2))
          r.copy(_1 = v)
        else
          r.copy(_2 = v)
      }

    def keyPressed(e: KeyEvent): Unit =
      (e.getKeyCode: @switch) match {
        case KeyEvent.VK_LEFT =>
          // println(s"VK_LEFT. isAltDown ${e.isAltDown} isShiftDown ${e.isShiftDown}")
          if (e.isAltDown) {
            if (e.isShiftDown) {
              expandRange()
            } else {
              if (slider.valueVisible && slider.valueEditable) {
                val m = slider.model
                val r = m.range
                if (m.value <= r._1)
                  m.value = m.minimum
                else if (m.value <= r._2)
                  m.value = r._1
                else
                  m.value = r._2
              }
            }
          } else {
            incValue(-1)
          }

        case KeyEvent.VK_RIGHT =>
          if (e.isAltDown) {
            if (e.isShiftDown) {
              expandRange()
            } else {
              if (slider.valueVisible && slider.valueEditable) {
                val m = slider.model
                val r = m.range
                if (m.value >= r._2)
                  m.value = m.maximum
                else if (m.value >= r._1)
                  m.value = r._2
                else
                  m.value = r._1
              }
            }
          } else {
            incValue(1)
          }

        case _ => // println(s"keyPressed. ${e.getKeyCode}")
      }

    def keyReleased(e: KeyEvent): Unit = ()
    def keyTyped   (e: KeyEvent): Unit = ()
  }

  private def installListeners(): Unit = {
    slider.addMouseListener       (trackListener)
    slider.addMouseMotionListener (trackListener)
    slider.addKeyListener         (trackListener)
    slider.addFocusListener       (trackListener)
    slider.addChangeListener      (trackListener)
  }

  private def uninstallListeners(): Unit = {
    slider.removeMouseListener       (trackListener)
    slider.removeMouseMotionListener (trackListener)
    slider.removeKeyListener         (trackListener)
    slider.removeFocusListener       (trackListener)
    slider.removeChangeListener      (trackListener)
  }

  override def paint(g: Graphics, c: JComponent): Unit = {
    recalculateIfInsetsChanged()
    // recalculateIfOrientationChanged()
    calcGeometry()

    val g2 = g.asInstanceOf[Graphics2D]
    // val clip = g.getClipBounds
    // val atOrig = g2.getTransform

    // val m   = slider.model
    val ty2 = trackRect.y + trackRect.height - 1

    g2.setColor(colrFillTrack)
    g2.fillRect(trackRect.x + 1, trackRect.y + 1, trackRect.width - 2, trackRect.height - 2)
    g2.setColor(colrDrawTrack)
    g2.drawRect(trackRect.x, trackRect.y, trackRect.width - 1, trackRect.height - 1)

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING  , RenderingHints.VALUE_ANTIALIAS_ON)

    val focused = slider.hasFocus

    if (slider.rangeVisible) {
      val wts = xHi - xLo - 1 // XXX
      if (wts > 0) {
        g2.setColor(colrFillSel)
        g2.fillRect(xLo + 1, trackRect.y, wts, trackRect.height)
      }
      g2.setColor(if (focused && focusHandle == LowHandle ) colrDrawHandleSel else colrDrawHandle)
      g2.drawLine(xLo, trackRect.y, xLo, ty2)
      g2.setColor(if (focused && focusHandle == HighHandle) colrDrawHandleSel else colrDrawHandle)
      g2.drawLine(xHi, trackRect.y, xHi, ty2)

      g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE     )
      g2.translate(xLo, trackRect.y) //  _insets.top)
      val fLo = focused && focusHandle == LowHandle
      g2.setPaint(if (fLo) pntFillRangeSel else pntFillRange)
      g2.fill(shpLow)
      g2.setColor(if (fLo) colrDrawHandleSel else colrDrawHandle)
      g2.draw(shpLow)
      g2.translate(xHi - xLo, 0)
      val fHi = focused && focusHandle == HighHandle
      g2.setPaint(if (fHi) pntFillRangeSel else pntFillRange)
      g2.fill(shpHigh)
      g2.setColor(if (fHi) colrDrawHandleSel else colrDrawHandle)
      g2.draw(shpHigh)
      g2.translate(-xHi, -trackRect.y) // -_insets.top)
      g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE)
    }

    if (slider.valueVisible) {
      g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
      g2.translate(xVal, trackRect.y) // _insets.top)
      val fVal = focused && focusHandle == ValueHandle
      g2.setPaint(if (fVal) pntFillValueSel else pntFillValue)
      g2.fill(shpValue)
      g2.setColor(if (fVal) colrDrawHandleSel else colrDrawHandle)
      g2.draw(shpValue)
      g2.translate(-xVal, -trackRect.y) // -_insets.top)
      g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE)

      g2.setColor(if (focused) colrDrawHandleSel else colrDrawHandle)
      g2.drawLine(xVal, trackRect.y + 2, xVal, ty2)
    }
  }

  private def getPreferredHorizontalSize: Dimension = {
    val dim = UIManager.getDimension("Slider.horizontalSize")
    if (dim == null)
      new Dimension(200, 17)
    else
      dim
  }

  private def getPreferredVerticalSize: Dimension = {
    val dim = UIManager.getDimension("Slider.verticalSize")
    if (dim == null)
      new Dimension(17, 200)
    else
      dim
  }

  private def getMinimumHorizontalSize: Dimension = {
    val dim = UIManager.getDimension("Slider.minimumHorizontalSize")
    if (dim == null)
      new Dimension(36, 17)
    else
      dim
  }

  private def getMinimumVerticalSize: Dimension = {
    val dim = UIManager.getDimension("Slider.minimumVerticalSize")
    if (dim == null)
      new Dimension(17, 36)
    else
      dim
  }

  protected def recalculateIfInsetsChanged(): Unit = {
    _insets = slider.getInsets
  }

  override def getPreferredSize(c: JComponent): Dimension = {
    recalculateIfInsetsChanged()
    if (slider.orientation == SwingConstants.VERTICAL) {
      val d     = new Dimension(getPreferredVerticalSize)
      d.width  += _insets.left + _insets.right
      d
    } else {
      val d = new Dimension(getPreferredHorizontalSize)
      d.height += _insets.top + _insets.bottom
      d
    }
  }

  override def getMinimumSize(c: JComponent): Dimension = {
    recalculateIfInsetsChanged()
    if (slider.orientation == SwingConstants.VERTICAL) {
      val d = new Dimension(getMinimumVerticalSize)
      d.width += _insets.left + _insets.right
      d
    } else {
      val d = new Dimension(getMinimumHorizontalSize)
      d.height += _insets.top + _insets.bottom
      d
    }
  }

  override def getMaximumSize(c: JComponent): Dimension = {
    val d = getPreferredSize(c)
    if (slider.orientation == SwingConstants.VERTICAL) {
      d.height = Short.MaxValue
    } else {
      d.width  = Short.MaxValue
    }
    d
  }
}