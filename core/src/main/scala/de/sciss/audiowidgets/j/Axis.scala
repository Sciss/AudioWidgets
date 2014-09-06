/*
 *  Axis.scala
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

package de.sciss.audiowidgets
package j

import java.awt.geom.{ GeneralPath, AffineTransform }
import java.awt.image.BufferedImage
import java.util.Locale
import annotation.switch
import java.awt.{Font, Color, Dimension, FontMetrics, Graphics, Graphics2D, Rectangle, RenderingHints, TexturePaint}
import javax.swing.{UIManager, SwingConstants, JComponent}

/**
 *  A GUI element for displaying
 *  the timeline's axis (ruler)
 *  which is used to display the
 *  time indices and to allow the
 *  user to position and select the
 *  timeline.
 *
 *	@todo		FIXEDBOUNDS is ignored in logarithmic mode now
 *	@todo		new label width calculation not performed in logarithmic mode
 *	@todo    detect font property changes
 */
object Axis {
  private final val DECIMAL_RASTER  = Array(100000000L, 10000000L, 1000000L, 100000L, 10000L, 1000L, 100L, 10L, 1L)
  private final val INTEGERS_RASTER = Array(100000000L, 10000000L, 1000000L, 100000L, 10000L, 1000L)
  private final val TIME_RASTER     = Array( 60000000L,  6000000L,  600000L,  60000L, 10000L, 1000L, 100L, 10L, 1L)
  private final val MinLabelSpace   = 16

  private final val pntBarGradientPixels = Array(
    0xFFB8B8B8, 0xFFC0C0C0, 0xFFC8C8C8, 0xFFD3D3D3,
    0xFFDBDBDB, 0xFFE4E4E4, 0xFFEBEBEB, 0xFFF1F1F1,
    0xFFF6F6F6, 0xFFFAFAFA, 0xFFFBFBFB, 0xFFFCFCFC,
    0xFFF9F9F9, 0xFFF4F4F4, 0xFFEFEFEF)
  private final val barExtent = pntBarGradientPixels.length

  private class Label(val name: String, val pos: Int)

  lazy val DefaultFont: Font = {
    val f = UIManager.getFont("Slider.font", Locale.US)
    if (f != null) f.deriveFont(math.min(f.getSize2D, 9.5f)) else new Font("SansSerif", Font.PLAIN, 9)
  }
}

class Axis(orient: Int = SwingConstants.HORIZONTAL)
  extends JComponent with AxisLike {

  import Axis._
  import SwingConstants.{HORIZONTAL, VERTICAL}

  private var _orient = orient

  private var recentWidth   = 0
  private var recentHeight  = 0
  private var doRecalc      = true

  private val kPeriod   = 1000.0
  private var labels    = new Array[Label](0)
  private val shpTicks  = new GeneralPath()

  private val trnsVertical = new AffineTransform()

  private var labelRaster: Array[Long] = null
  private var labelMinRaster = 0L

  private var spcMin = 0.0
  private var spcMax = 0.0 // 1.0

  private var formatVar: AxisFormat = AxisFormat.Decimal
  private var flMirror      = false
  private var flTimeFormat  = false
  private var flTimeHours   = false
  private var flTimeMillis  = false
  private var flIntegers    = false
  private var flFixedBounds = false

  private var imgWidth  = 0
  private var imgHeight = 0
  private var img: BufferedImage = null
  private var pntBackground: TexturePaint = null

  private def orientUpdated(): Unit = {
    (_orient: @switch) match {
      case HORIZONTAL =>
        setMaximumSize  (new Dimension(getMaximumSize.width, barExtent))
        setMinimumSize  (new Dimension(getMinimumSize.width, barExtent))
        setPreferredSize(new Dimension(getPreferredSize.width, barExtent))
        imgWidth  = 1
        imgHeight = barExtent
      case VERTICAL =>
        setMaximumSize  (new Dimension(barExtent, getMaximumSize.height))
        setMinimumSize  (new Dimension(barExtent, getMinimumSize.height))
        setPreferredSize(new Dimension(barExtent, getPreferredSize.height))
        imgWidth  = barExtent
        imgHeight = 1
    }
    if (img != null) img.flush()
    img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB)
    img.setRGB(0, 0, imgWidth, imgHeight, pntBarGradientPixels, 0, imgWidth)
    pntBackground = new TexturePaint(img, new Rectangle(0, 0, imgWidth, imgHeight))
    triggerRedisplay()
  }

  //     private var viewPortVar: Option[ JViewport ] = None

  // ---- constructor ----
  orientUpdated()
  flagsUpdated()

  setFont(Axis.DefaultFont)
  setOpaque(true)

  //   def viewport : Option[ JViewport ] = viewPortVar
  //   def viewport_=( v: JViewport ): Unit = viewPortVar = v

  //   def flags = flagsVar
  //	def flags_=( newFlags: Int ): Unit = {
  //		if( flagsVar == newFlags ) return
  //        flagsVar = newFlags
  //        flagsUpdated()
  //    }

  def orientation: Int = _orient
  def orientation_=(orient: Int): Unit = if (_orient != orient) {
    if (orient != HORIZONTAL && orient != VERTICAL) throw new IllegalArgumentException(orient.toString)
    _orient = orient
    orientUpdated()
  }

  /**
   * Flag: Defines the axis to have flipped min/max values.
   * I.e. for horizontal orient, the maximum value
   * corresponds to the left edge, for vertical orient
   * the maximum corresponds to the bottom edge
   */
  def inverted: Boolean = flMirror
  def inverted_=(b: Boolean): Unit = if (flMirror != b) {
    flMirror = b
    flagsUpdated()
  }

  /*
   *	Flag: Requests that the space's min and max are always displayed
   *		  and hence subdivision are made according to the bounds
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

  //   /**
  //    *	Flag: Requests the labels to be formatted as MIN:SEC.MILLIS
  //    */
  //   def timeFormat : Boolean = flTimeFormat
  //   def timeFormat_=( b: Boolean ): Unit = {
  //      if( flTimeFormat != b ) {
  //         flTimeFormat = b
  //         flagsUpdated()
  //      }
  //   }
  //
  //	/*
  //	 *	Flag: Requests that the label values be integers
  //	 */
  //   def intFormat : Boolean = flIntegers
  //   def intFormat_=( b: Boolean ): Unit = {
  //      if( flIntegers != b ) {
  //         flIntegers = b
  //         if( b ) flTimeFormat = false
  //         flagsUpdated()
  //      }
  //   }

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

  //   protected def setSpaceNoRepaint( newSpace: VectorSpace ): Unit = {
  //      spaceVar = newSpace
  //		doRecalc = true
  //   }

  private val normalRect = new Rectangle

  private def normalBounds: Rectangle = {
    normalRect.x      = 0
    normalRect.y      = 0
    normalRect.width  = getWidth
    normalRect.height = getHeight
    normalRect
  }

  //   private def portBounds: Rectangle = {
  //      val r = viewPortVar.get.getViewRect
  //      if( r != normalRect ) {
  //          normalRect.setBounds( r )
  //          viewRectChanged( r )
  //      }
  //      normalRect
  //   }

  //   // subclasses might want to use this
  //   protected def viewRectChanged( r: Rectangle ) = ()

  override def paintComponent(g: Graphics): Unit = {
    super.paintComponent(g)

    val g2        = g.asInstanceOf[Graphics2D]
    val trnsOrig  = g2.getTransform
    val fm        = g2.getFontMetrics

    val r = /* if( viewPortVar.isEmpty ) */ normalBounds /* else portBounds */

    if (doRecalc || (r.width != recentWidth) || (r.height != recentHeight)) {
      recentWidth   = r.width
      recentHeight  = r.height
      recalcLabels(g)
      if (_orient == VERTICAL) recalcTransforms()
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
    g2.setColor(Color.lightGray)
    g2.draw(shpTicks)

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setColor(Color.black)

    var i = 0
    while (i < labels.length) {
      val l = labels(i)
      g2.drawString(l.name, l.pos, y)
      i += 1
    }

    g2.setTransform(trnsOrig)
    //      paintOnTop( g2 )
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aaOrig)
  }

  private def recalcTransforms(): Unit =
    trnsVertical.setToRotation(-math.Pi / 2, recentHeight.toDouble / 2, recentHeight.toDouble / 2)

  private def calcStringWidth(decimals: Int, fntMetr: FontMetrics, value: Double): Int = {
    val s = format.format(value, decimals = decimals, pad = 0)
    fntMetr.stringWidth(s)
  }

  private def calcMinLabSpc(decimals: Int, fntMetr: FontMetrics, mini: Double, maxi: Double): Int = {
    math.max(calcStringWidth(decimals, fntMetr, mini), calcStringWidth(decimals, fntMetr, maxi)) + MinLabelSpace
  }

  private def recalcLabels(g: Graphics): Unit = {
    import math._

    val fntMetr = g.getFontMetrics

    shpTicks.reset()
    if (spcMin == spcMax) {
      labels = new Array[Label](0)
      return
    }

    val (width, height) = if (_orient == HORIZONTAL) {
      //			if( spaceVar.hlog ) {
      //				recalcLogLabels
      //				return
      //			}
      (recentWidth, recentHeight) // , spaceVar.hmin, spaceVar.hmax
    } else {
      //			if( spaceVar.vlog ) {
      //				recalcLogLabels
      //				return
      //			}
      (recentHeight, recentWidth) // , spaceVar.vmin, spaceVar.vmax
    }
    val scale = width / (spcMax - spcMin)
    val minK  = kPeriod * spcMin
    val maxK  = kPeriod * spcMax

    val isInteger = flIntegers || (flTimeFormat && !flTimeMillis)
    var (decimals: Int, numTicks: Int, valueOff: Double, pixelOff: Double, valueStep: Double) =
      if (flFixedBounds) {
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
        val minLbDist = calcMinLabSpc(decimals1, fntMetr, spcMin, spcMax)
        var numLabels = max(1, width / minLbDist)

        // ok, easy way : only divisions by powers of two
        var shift = 0
        while (numLabels > 2) {
          shift      += 1
          numLabels >>= 1
        }
        numLabels   <<= shift
        val valueStep = (maxK - minK) / numLabels

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

        val decimals3 = if (decimals2 == decimals1) decimals2 else {
          // ok, labels get bigger, recalc numLabels ...
          val minLbDist = calcMinLabSpc(decimals2, fntMetr, spcMin, spcMax)
          numLabels = max(1, width / minLbDist)
          shift = 0
          while (numLabels > 2) {
            shift += 1
            numLabels >>= 1
          }
          numLabels <<= shift
          val valueStep = (maxK - minK) / numLabels

          // nochmal ptrnIdx berechnen, evtl. reduziert sich die aufloesung wieder...
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

        (decimals3, 4, minK, 0, valueStep)

      } else {
        // ---- no fixed bounds ----

        // make a first label width calculation with coarsest display
        var minLbDist = calcMinLabSpc(0, fntMetr, spcMin, spcMax)
        var numLabels = max(1, width / minLbDist)

        // now valueStep =^= 1000 * minStep
        var valueStep = ceil((maxK - minK) / numLabels)
        // die Grossenordnung von valueStep ist Indikator fuer Message Pattern
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
          minLbDist = max(calcStringWidth(decimals1, fntMetr, spcMin),
                          calcStringWidth(decimals1, fntMetr, spcMax)) + MinLabelSpace
          numLabels = max(1, width / minLbDist)
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
        val pixelOff = (valueOff - minK) / kPeriod * scale // + 0.5 (was a left over from quartz rendering)

        (decimals1, numTicks, valueOff, pixelOff, valueStep)
      }

    val pixelStep = valueStep / kPeriod * scale
    var tickStep = pixelStep / numTicks
    val numLabels = max(0, ((width - pixelOff + pixelStep - 1.0) / pixelStep).toInt)

    if (labels.length != numLabels) labels = new Array[Label](numLabels)

    if (flMirror) {
      pixelOff = width - pixelOff
      tickStep = -tickStep
    }

    var i = 0
    while (i < numLabels) {
      labels(i) = new Label(format.format(valueOff / kPeriod, decimals = decimals, pad = 0), (pixelOff + 2).toInt)
      valueOff += valueStep
      shpTicks.moveTo(pixelOff.toFloat, 1)
      shpTicks.lineTo(pixelOff.toFloat, height - 2)
      pixelOff += tickStep
      var k = 1
      while (k < numTicks) {
        shpTicks.moveTo(pixelOff.toFloat, height - 4)
        shpTicks.lineTo(pixelOff.toFloat, height - 2)
        pixelOff += tickStep
        k += 1
      }
      i += 1
    }
  }

  private def triggerRedisplay(): Unit = {
    doRecalc = true
    //		if( host.isDefined ) {
    //			host.get.update( this )
    //		} else
    if (isVisible) {
      repaint()
    }
  }

  // -------------- Disposable interface --------------

  def dispose(): Unit = {
    labels = null
    shpTicks.reset()
    img.flush()
  }
}