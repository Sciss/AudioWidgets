/*
 *  Axis.scala
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

import java.awt.geom.{AffineTransform, GeneralPath}
import java.awt.{Color, Dimension, Font, FontMetrics, Graphics, Graphics2D, LinearGradientPaint, Paint, Rectangle, RenderingHints}
import java.util.Locale
import javax.swing.{JComponent, SwingConstants, UIManager}

import scala.annotation.switch

/** A GUI element for displaying
  * the timeline's axis (ruler)
  * which is used to display the
  * time indices and to allow the
  * user to position and select the
  * timeline.
  *
  * @todo		FIXEDBOUNDS is ignored in logarithmic mode now
  * @todo		new label width calculation not performed in logarithmic mode
  * @todo    detect font property changes
  */
object Axis {
  private final val DECIMAL_RASTER  = Array(100000000L, 10000000L, 1000000L, 100000L, 10000L, 1000L, 100L, 10L, 1L)
  private final val INTEGERS_RASTER = Array(100000000L, 10000000L, 1000000L, 100000L, 10000L, 1000L)
  private final val TIME_RASTER     = Array( 60000000L,  6000000L,  600000L,  60000L, 10000L, 1000L, 100L, 10L, 1L)
  private final val MinLabelSpace   = 16

  private type ColorScheme = Array[Color]

  private final val lightScheme: ColorScheme = Array(new Color(0xB8B8B8), new Color(0xFCFCFC), new Color(0xEFEFEF))
  private final val darkScheme : ColorScheme = Array(new Color(0x080808), new Color(0x2E2E2E), new Color(0x272727))

  private final val gradFractions = Array[Float](0.0f, 0.75f, 0.9375f)

  private final val barExtent = 15

  private class Label(val name: String, val pos: Int)

  lazy val DefaultFont: Font = {
    val f = UIManager.getFont("Slider.font", Locale.US)
    if (f != null) f.deriveFont(math.min(f.getSize2D, 9.5f)) else new Font("SansSerif", Font.PLAIN, 9)
  }
}

class Axis(orient: Int = SwingConstants.HORIZONTAL)
  extends JComponent with AxisLike {

  import SwingConstants.{HORIZONTAL, VERTICAL}

  import Axis._

  private[this] var _orient = orient

  private[this] var recentWidth   = 0
  private[this] var recentHeight  = 0
  private[this] var doRecalc      = true

  private[this] val kPeriod   = 1000.0
  private[this] var labels    = new Array[Label](0)
  private[this] val shpTicks  = new GeneralPath()

  private[this] val trnsVertical = new AffineTransform()

  private[this] var labelRaster: Array[Long] = _
  private[this] var labelMinRaster = 0L

  private[this] var spcMin = 0.0
  private[this] var spcMax = 0.0 // 1.0

  private[this] var formatVar: AxisFormat = AxisFormat.Decimal
  private[this] var flMirror      = false
  private[this] var flTimeFormat  = false
  private[this] var flTimeHours   = false
  private[this] var flTimeMillis  = false
  private[this] var flIntegers    = false
  private[this] var flFixedBounds = false

  private[this] var pntBackground: Paint = _

  private[this] final val scheme    = if (Util.isDarkSkin) darkScheme else lightScheme
  private[this] final val colrTicks = if (Util.isDarkSkin) Color.gray else Color.lightGray

  private def orientUpdated(): Unit = {
    val isHoriz = _orient match {
      case HORIZONTAL =>
        setMaximumSize  (new Dimension(getMaximumSize  .width, barExtent))
        setMinimumSize  (new Dimension(getMinimumSize  .width, barExtent))
        setPreferredSize(new Dimension(getPreferredSize.width, barExtent))
//        imgWidth  = 1
//        imgHeight = barExtent
        true
      case VERTICAL =>
        setMaximumSize  (new Dimension(barExtent, getMaximumSize  .height))
        setMinimumSize  (new Dimension(barExtent, getMinimumSize  .height))
        setPreferredSize(new Dimension(barExtent, getPreferredSize.height))
//        imgWidth  = barExtent
//        imgHeight = 1
        false
    }
//    if (img != null) img.flush()
//    img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB)
//    img.setRGB(0, 0, imgWidth, imgHeight, pntBarGradientPixels, 0, imgWidth)
//    pntBackground = new TexturePaint(img, new Rectangle(0, 0, imgWidth, imgHeight))
    pntBackground = new LinearGradientPaint(0f, 0f, if (isHoriz) 0f else barExtent, if (isHoriz) barExtent else 0,
      gradFractions, scheme)
    triggerRedisplay()
  }

  // ---- constructor ----
  orientUpdated()
  flagsUpdated()

  setFont(Axis.DefaultFont)
  setOpaque(true)

  def orientation: Int = _orient
  def orientation_=(orient: Int): Unit = if (_orient != orient) {
    if (orient != HORIZONTAL && orient != VERTICAL) throw new IllegalArgumentException(orient.toString)
    _orient = orient
    orientUpdated()
  }

  /** Flag: Defines the axis to have flipped min/max values.
    * I.e. for horizontal orient, the maximum value
    * corresponds to the left edge, for vertical orient
    * the maximum corresponds to the bottom edge
    */
  def inverted: Boolean = flMirror
  def inverted_=(b: Boolean): Unit = if (flMirror != b) {
    flMirror = b
    flagsUpdated()
  }

  /** Flag: Requests that the space's min and max are always displayed
    * and hence subdivision are made according to the bounds
    */
  def fixedBounds: Boolean = flFixedBounds
  def fixedBounds_=(b: Boolean): Unit = if (flFixedBounds != b) {
    flFixedBounds = b
    flagsUpdated()
  }

  def format: AxisFormat = formatVar
  def format_=(f: AxisFormat): Unit = if (formatVar != f) {
    formatVar = f
    f match {
      case AxisFormat.Integer =>
        flIntegers    = true
        flTimeFormat  = false
      case AxisFormat.Time(hours, millis) =>
        flIntegers    = false
        flTimeFormat  = true
        flTimeHours   = hours
        flTimeMillis  = millis
      case AxisFormat.Decimal =>
        flIntegers    = false
        flTimeFormat  = false
    }
    flagsUpdated()
  }

  private def flagsUpdated(): Unit = {
    //	   flMirroir		= (flags & MIRROIR) != 0
    //		flTimeFormat	= (flags & TIMEFORMAT) != 0
    //		flIntegers		= (flags & INTEGERS) != 0
    //		flFixedBounds	= (flags & FIXEDBOUNDS) != 0

    if (flTimeFormat) {
      labelRaster = TIME_RASTER
    } else {
      labelRaster = if (flIntegers) INTEGERS_RASTER else DECIMAL_RASTER
    }
    labelMinRaster = labelRaster(labelRaster.length - 1)

    triggerRedisplay()
  }

  def minimum: Double = spcMin
  def minimum_=(value: Double): Unit = if (value != spcMin) {
    spcMin = value
    triggerRedisplay()
  }

  def maximum: Double = spcMax
  def maximum_=(value: Double): Unit =if (value != spcMax) {
    spcMax = value
    triggerRedisplay()
  }

  private val normalRect = new Rectangle

  private def normalBounds: Rectangle = {
    normalRect.x      = 0
    normalRect.y      = 0
    normalRect.width  = getWidth
    normalRect.height = getHeight
    normalRect
  }

  override def paintComponent(g: Graphics): Unit = {
    super.paintComponent(g)

    val g2        = g.asInstanceOf[Graphics2D]
    val trnsOrig  = g2.getTransform
    val fm        = g2.getFontMetrics

    val r = /* if( viewPortVar.isEmpty ) */ normalBounds /* else portBounds */

    if (doRecalc || (r.width != recentWidth) || (r.height != recentHeight)) {
      recentWidth   = r.width
      recentHeight  = r.height
      recalculateLabels(g)
      if (_orient == VERTICAL) recalculateTransforms()
      doRecalc = false
    }

    g2.setPaint(pntBackground)
    g2.fillRect(r.x, r.y, r.width, r.height)

    g2.translate(r.x, r.y)
    val aaOrig = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING)
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)

    val y = if (orient == VERTICAL) {
      g2.transform(trnsVertical)
      r.width - 2 /* 3 */ - fm.getMaxDescent
    } else {
      r.height - 2 /* 3 */ - fm.getMaxDescent
    }
    g2.setColor(colrTicks)
    g2.draw(shpTicks)

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setColor(getForeground)

    var i = 0
    while (i < labels.length) {
      val l = labels(i)
      g2.drawString(l.name, l.pos, y)
      i += 1
    }

    g2.setTransform(trnsOrig)
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aaOrig)
  }

  private def recalculateTransforms(): Unit =
    trnsVertical.setToRotation(-math.Pi / 2, recentHeight.toDouble / 2, recentHeight.toDouble / 2)

  private def calcStringWidth(decimals: Int, fntMetrics: FontMetrics, value: Double): Int = {
    val s = format.format(value, decimals = decimals)
    fntMetrics.stringWidth(s)
  }

  private def calcMinLabSpc(decimals: Int, fntMetrics: FontMetrics, mini: Double, maxi: Double): Int =
    math.max(calcStringWidth(decimals, fntMetrics, mini), calcStringWidth(decimals, fntMetrics, maxi)) + MinLabelSpace

  private def recalculateLabels(g: Graphics): Unit = {

    shpTicks.reset()
    if (spcMin == spcMax) {
      labels = new Array[Label](0)
    } else {
      val isHoriz   = _orient == HORIZONTAL
      val length    = if (isHoriz) recentWidth  else recentHeight
      val thickness = if (isHoriz) recentHeight else recentWidth
      val scale     = length / (spcMax - spcMin)
      val minK      = kPeriod * spcMin
      val maxK      = kPeriod * spcMax
      val isInteger = flIntegers || (flTimeFormat && !flTimeMillis)

      if (flFixedBounds) {
        recalculateLabelsFixed  (g, minK = minK, maxK = maxK, isInteger = isInteger,
          length = length, thickness = thickness, scale = scale)
      } else {
        recalculateLabelsElastic(g, minK = minK, maxK = maxK, isInteger = isInteger,
          length = length, thickness = thickness, scale = scale)
      }
    }
  }

  private def recalculateLabelsFixed(g: Graphics, minK: Double, maxK: Double, isInteger: Boolean,
                                     length: Int, thickness: Int, scale: Double): Unit = {
    import math._

    val fntMetrics = g.getFontMetrics

    val decimals1 = if (isInteger) 0
    else {
      val decTmp = {
        val n = abs(minK).toLong
        if ((n % 1000) == 0) {
          0
        } else if ((n % 100) == 0) {
          1
        } else if ((n % 10) == 0) {
          2
        } else {
          3
        }
      }

      val n = abs(maxK).toLong
      if ((n % 1000) == 0) {
        decTmp
      } else if ((n % 100) == 0) {
        max(decTmp, 1)
      } else if ((n % 10) == 0) {
        max(decTmp, 2)
      } else {
        3
      }
    }

    // make a first label width calculation with coarsest display
    val minLbDist = calcMinLabSpc(decimals1, fntMetrics, spcMin, spcMax)
    var numLabels = max(1, length / minLbDist)

    // ok, easy way : only divisions by powers of two
    var shift = 0
    while (numLabels > 2) {
      shift      += 1
      numLabels >>= 1
    }
    numLabels   <<= shift
    var valueStep = (maxK - minK) / numLabels

    val decimals2 = if (isInteger) 0
    else {
      val n = valueStep.toLong
      if ((n % 1000) == 0) {
        decimals1
      } else if ((n % 100) == 0) {
        max(decimals1, 1)
      } else if ((n % 10) == 0) {
        max(decimals1, 2)
      } else {
        3
      }
    }
//    println(f"[2] minLbDist $minLbDist, numLabels = $numLabels, decimals $decimals2")

    val decimals3 = if (decimals2 == decimals1) decimals2 else {
      // ok, labels get bigger, recalculate numLabels ...
      val minLbDist = calcMinLabSpc(decimals2, fntMetrics, spcMin, spcMax)
      numLabels = max(1, length / minLbDist)
      shift = 0
      while (numLabels > 2) {
        shift += 1
        numLabels >>= 1
      }
      numLabels <<= shift
      valueStep = (maxK - minK) / numLabels

      // calculate ptrnIdx again, possibly reducing the resolution again...
      val n = valueStep.toLong
      val res = if ((n % 1000) == 0) {
        decimals1
      } else if ((n % 100) == 0) {
        max(decimals1, 1)
      } else if ((n % 10) == 0) {
        max(decimals1, 2)
      } else {
        3
      }

//      println(f"[3] minLbDist $minLbDist, numLabels = $numLabels, decimals $res")
      res
    }

    labelsFinish(g = g, length = length, thickness = thickness, scale = scale, decimals = decimals3, numTicks = 4,
      valueOff0 = minK, pixelOff0 = 0, valueStep = valueStep)
  }

  private def recalculateLabelsElastic(g: Graphics, minK: Double, maxK: Double, isInteger: Boolean,
                                       length: Int, thickness: Int, scale: Double): Unit = {
    import math._

    // make a first label width calculation with coarsest display
    val fntMetrics  = g.getFontMetrics
    var minLbDist   = calcMinLabSpc(0, fntMetrics, spcMin, spcMax)
    var numLabels   = max(1, length / minLbDist)

    // now valueStep =^= 1000 * minStep
    var valueStep = ceil((maxK - minK) / numLabels)
    // the magnitude of valueStep is an indicator for message pattern
    var decimals1 = if (isInteger) 0 else 3
    var raster = labelMinRaster
    var i = 0
    var break = false
    while ((i < labelRaster.length) && !break) {
      if (valueStep >= labelRaster(i)) {
        decimals1  = max(0, i - 5)
        raster    = labelRaster(i)
        break     = true
      } else {
        i += 1
      }
    }
    if (decimals1 > 0) {
      // have to recheck label width!
      val w1 = calcStringWidth(decimals1, fntMetrics, spcMin)
      val w2 = calcStringWidth(decimals1, fntMetrics, spcMax)
      minLbDist = max(w1, w2) + MinLabelSpace
      numLabels = max(1, length / minLbDist)
      valueStep = ceil((maxK - minK) / numLabels)
    }
    valueStep = max(1, floor((valueStep + raster - 1) / raster))
    if (valueStep == 7 || valueStep == 9) valueStep = 10

    val numTicks = (valueStep.toInt: @switch) match {
      case 2 => 4
      case 4 => 4
      case 8 => 4
      case 3 => 6
      case 6 => 6
      case _ => 5
    }
    valueStep *= raster
    val valueOff = floor(abs(minK) / valueStep) * (if (minK >= 0) valueStep else -valueStep)
    val pixelOff = (valueOff - minK) / kPeriod * scale

    labelsFinish(g = g, length = length, thickness = thickness, scale = scale,
      decimals = decimals1, numTicks = numTicks, valueOff0 = valueOff, pixelOff0 = pixelOff, valueStep = valueStep)
  }

  private def labelsFinish(g: Graphics, length: Int, thickness: Int, scale: Double, decimals: Int, numTicks: Int,
                           valueOff0: Double, pixelOff0: Double, valueStep: Double): Unit = {
    import math._
    var valueOff  = valueOff0
    var pixelOff  = pixelOff0
    val pixelStep = valueStep / kPeriod * scale
    var tickStep  = pixelStep / numTicks
    val numLabels = max(0, ((length - pixelOff + pixelStep - 1.0) / pixelStep).toInt)
//    val numLabels = max(0, ((length - pixelOff) / pixelStep).toInt)
//    println(f"scale $scale%g, decimals $decimals, numTicks $numTicks, pixelStep $pixelStep%g, tickStep $tickStep%g, numLabels $numLabels")

    if (labels.length != numLabels) labels = new Array[Label](numLabels)

    if (flMirror) {
      pixelOff = length - pixelOff
      tickStep = -tickStep
    }

    var i = 0
    while (i < numLabels) {
      labels(i) = new Label(format.format(valueOff / kPeriod, decimals = decimals), (pixelOff + 2).toInt)
      valueOff += valueStep
      shpTicks.moveTo(pixelOff.toFloat, 1)
      shpTicks.lineTo(pixelOff.toFloat, thickness - 2)
      pixelOff += tickStep
      var k = 1
      while (k < numTicks) {
        shpTicks.moveTo(pixelOff.toFloat, thickness - 4)
        shpTicks.lineTo(pixelOff.toFloat, thickness - 2)
        pixelOff += tickStep
        k += 1
      }
      i += 1
    }
  }

  private def triggerRedisplay(): Unit = {
    doRecalc = true
    if (isVisible) {
      repaint()
    }
  }

  // -------------- Disposable interface --------------

  def dispose(): Unit = {
    labels = null
    shpTicks.reset()
  }
}