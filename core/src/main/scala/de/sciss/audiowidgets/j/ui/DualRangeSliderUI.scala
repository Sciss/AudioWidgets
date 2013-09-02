package de.sciss.audiowidgets
package j
package ui

import javax.swing.plaf.ComponentUI
import javax.swing.{SwingConstants, UIManager, JComponent}
import java.awt.{Insets, Dimension, Graphics2D, Color, Graphics}

// XXX TODO: only handles horizontal orientation at the moment
object DualRangeSliderUI {
  private final val colrFillTrack     = new Color(0x00, 0x00, 0x00, 0x26)
  private final val colrDrawTrack     = new Color(0x00, 0x00, 0x00, 0x4C)
  private final val colrFillSel       = new Color(0x00, 0x00, 0x00, 0x3B) // 0x59
  private final val colrDrawHandle    = new Color(0x73, 0x76, 0x68) // XXX TODO: depends on user colour
  private final val colrDrawHandleSel = new Color(0x33, 0x33, 0x33)

  // private final var colrDrawValue   = new Color(0x00, 0x00, 0x00, 0x73)
  // HSB thumb outline un-selected: 73, 12, 46
  // HSB thumb fill gradient: low = 72, 12, 87 ; high = 69, 9, 90
  // reference color: 0xCC, 0xD2, B0, or HSB 71, 16, 82
  // RGB thumb fill gradient selected: high = 0x6A, 0x6A, 0x6A (0x6D); low = 0x4D, 0x4D, 0x4D (0x41)

  // sizes: thumb height = 8; insets: top 4; bottom 5; left and right 8

  // private val shp

  private sealed trait MaybeHandle {
    def valueOption(m: DualRangeModel): Option[Int]
  }
  private case object NoHandle extends MaybeHandle {
    def valueOption(m: DualRangeModel) = None
  }
  private sealed trait Handle extends MaybeHandle {
    def value(m: DualRangeModel): Int
    def valueOption(m: DualRangeModel) = Some(value(m))
  }
  private case object ValueHandle extends Handle {
    def value(m: DualRangeModel) = m.value
  }
  private case object LowHandle extends Handle {
    def value(m: DualRangeModel) = m.range._1
  }
  private case object HighHandle extends Handle {
    def value(m: DualRangeModel) = m.range._2
  }
}
class DualRangeSliderUI(slider: DualRangeSlider) extends ComponentUI {
  import DualRangeSliderUI._

  private var handle: MaybeHandle = NoHandle
  private var _insets: Insets = null

  private var isDragging = false

  override def installUI(c: JComponent): Unit = {
    isDragging              = false
    //    trackListener           = createTrackListener()
    //    changeListener          = createChangeListener()
    //    componentListener       = createComponentListener()
    //    focusListener           = createFocusListener()
    //    scrollListener          = createScrollListener()
    //    propertyChangeListener  = createPropertyChangeListener()

    //    installDefaults()
    //    installListeners()
    //    installKeyboardActions()

    _insets = slider.getInsets
    // leftToRightCache = slider.getComponentOrientation.isLeftToRight
    //    focusRect     = new Rectangle()
    //    contentRect   = new Rectangle()
    //    labelRect     = new Rectangle()
    //    tickRect      = new Rectangle()
    //    trackRect     = new Rectangle()
    //    thumbRect     = new Rectangle()
    //    lastValue     = slider.getValue()

    // calculateGeometry()
  }

  override def paint(g: Graphics, c: JComponent): Unit = {
    recalculateIfInsetsChanged()
    // recalculateIfOrientationChanged()

    // val clip = g.getClipBounds
    val m   = slider.model
    val cw  = c.getWidth - (_insets.left + _insets.right)
    val tw  = cw - 16
    val tx  = 8 + _insets.left
    val ty  = 4 + _insets.top
    val th  = 8

    val g2 = g.asInstanceOf[Graphics2D]
    g2.setColor(colrFillTrack)
    g2.fillRect(tx + 1, ty + 1, tw - 2, th - 2)
    g2.setColor(colrDrawTrack)
    g2.drawRect(tx, ty, tw - 1, th - 1)

    val scale   = tw.toDouble / (m.maximum - m.minimum)

    if (slider.rangeVisible) {
      val r   = m.range
      val xLo = (r._1    * scale).toInt + tx
      val xHi = (r._2    * scale).toInt + tx
      val wts = xHi - xLo - 2
      if (wts > 0) {
        g2.setColor(colrFillSel)
        g2.fillRect(xLo + 1, 4, wts, 8)
      }
      g2.setColor(if (handle == LowHandle ) colrDrawHandleSel else colrDrawHandle)
      g2.drawLine(xLo, 4, xLo, 11)
      g2.setColor(if (handle == HighHandle) colrDrawHandleSel else colrDrawHandle)
      g2.drawLine(xHi, 4, xHi, 11)
    }

    if (slider.valueVisible) {
      val xVal  = (m.value * scale).toInt + tx
      g2.setColor(colrDrawHandleSel)
      g2.drawLine(xVal, 4, xVal, 11)
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
      val in = slider.getInsets
      if (_insets != in) {
        _insets = in
        // calculateGeometry()
      }
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
      d.width = Short.MaxValue
    }
    d
  }
}
