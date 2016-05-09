/*
 *  PeakMeterBar.scala
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

import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import javax.swing.{BorderFactory, JComponent, SwingConstants}
import java.awt.image.BufferedImage
import java.awt.{Color, Container, Dimension, Graphics, Graphics2D, Insets, Paint, Rectangle, TexturePaint}

/** A level (volume) meter GUI component. The component
  * is a vertical bar displaying a green-to-reddish bar
  * for the peak amplitude and a blue bar for RMS value.
  *
  * To animate the bar, call `setPeakAndRMS` at a
  * regular interval, typically around every 30 milliseconds
  * for a smooth look.
  *
  * @todo	allow linear display (now it's hard coded logarithmic)
  * @todo	add optional horizontal orientation
  */
object PeakMeterBar {
  private final val logRMSCorr  = 10.0 / math.log(10)
  private final val logPeakCorr = 2 * logRMSCorr // 20.0 / math.log(10)

  private final val bgPixels = Array(
    0xFF000000, 0xFF343434, 0xFF484848, 0xFF5C5C5C, 0xFF5C5C5C,
    0xFF5C5C5C, 0xFF5C5C5C, 0xFF5C5C5C, 0xFF484848, 0xFF343434,
    0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000,
    0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000)

  private final val rmsTopColor = Array(
    0x000068, 0x5537B9, 0x764EE5, 0x9062E8, 0x8B63E8,
    0x8360E8, 0x7C60E8, 0x8876EB, 0x594CB4, 0x403A63)

  private final val rmsBotColor = Array(
    0x000068, 0x2F4BB6, 0x4367E2, 0x577FE5, 0x577AE5,
    0x5874E6, 0x596FE6, 0x6B7AEA, 0x4851B1, 0x393D62)

  private final val peakTopColor = Array(
    0x000000, 0xB72929, 0xFF3C3C, 0xFF6B6B, 0xFF6B6B,
    0xFF6B6B, 0xFF6B6B, 0xFFA7A7, 0xFF3C3C, 0xB72929)

  private final val peakBotColor = Array(
    0x000000, 0x008E00, 0x00C800, 0x02FF02, 0x02FF02,
    0x02FF02, 0x02FF02, 0x68FF68, 0x00C800, 0x008E00)

   private def widenPixV(src: Array[Int], srcBreadth: Int, dstBreadth: Int, len: Int): Array[Int] = {
     val minBreadth   = math.min(srcBreadth, dstBreadth)
     val minBreadthH  = minBreadth >> 1
     val minBreadthH1 = minBreadth - minBreadthH
     val numWiden     = dstBreadth - srcBreadth
     val dst          = new Array[Int](dstBreadth * len)

     var y       = 0
     var srcOffL = 0
     var srcOffR = srcBreadth - minBreadthH1
     var dstOffL = 0
     var dstOffR = dstBreadth - minBreadthH1
     while (y < len) {
       System.arraycopy(src, srcOffL, dst, dstOffL, minBreadthH)
       System.arraycopy(src, srcOffR, dst, dstOffR, minBreadthH1)
       y       += 1
       srcOffL += srcBreadth
       srcOffR += srcBreadth
       dstOffL += dstBreadth
       dstOffR += dstBreadth
     }
     if (numWiden > 0) {
       y       = 0
       srcOffL = minBreadthH
       dstOffL = minBreadthH
       while (y < len) {
         val p = src(srcOffL)
         dstOffR = dstOffL + numWiden
         while (dstOffL < dstOffR) {
           dst(dstOffL) = p
           dstOffL     += 1
         }
         y       += 1
         srcOffL += srcBreadth
         dstOffL += srcBreadth
       }
     }
     dst
   }

  private def widenPixH(src: Array[Int], srcBreadth: Int, dstBreadth: Int, len: Int): Array[Int] = {
    val minBreadth   = math.min(srcBreadth, dstBreadth)
    val minBreadthH  = minBreadth >> 1
    val minBreadthH1 = minBreadth - minBreadthH
    val breadthDOff  = dstBreadth - minBreadthH1
    val breadthSOff  = srcBreadth - minBreadthH1
    val dst          = new Array[Int](dstBreadth * len)

    var dstOff = 0
    var y      = 0
    while (y < minBreadthH) {
      var x      = 0
      var srcOff = y
      while (x < len) {
        dst(dstOff) = src(srcOff)
        x          += 1
        dstOff     += 1
        srcOff     += srcBreadth
      }
      y += 1
    }
    while (y < breadthDOff) {
      var x = 0
      var srcOff = minBreadthH
      while (x < len) {
        dst(dstOff) = src(srcOff)
        x          += 1
        dstOff     += 1
        srcOff     += srcBreadth
      }
      y += 1
    }
    var srcOffS = breadthSOff
    while (y < dstBreadth) {
      var x = 0
      var srcOff = srcOffS
      while (x < len) {
        dst(dstOff) = src(srcOff)
        x          += 1
        dstOff     += 1
        srcOff     += srcBreadth
      }
      y       += 1
      srcOffS += 1
    }
    dst
  }

  private def hsbFade(breadth: Int, len: Int, topColor: Array[Int], botColor: Array[Int], vertical: Boolean): Array[Int] = {
    val pix       = new Array[Int](breadth * len)
    val hsbTop    = new Array[Float](3)
    val hsbBot    = new Array[Float](3)
    val w3        = 1.0f / (len - 2)
    val best      = breadth == 10
    val sTopColor = if (best) topColor else widenPixV(topColor, 10, breadth, 1)
    val sBotColor = if (best) botColor else widenPixV(botColor, 10, breadth, 1)

    var i = 0
    while (i < breadth) {
      val rgbT = sTopColor(i)
      Color.RGBtoHSB((rgbT >> 16) & 0xFF, (rgbT >> 8) & 0xFF, rgbT & 0xFF, hsbTop)
      val rgbB = sBotColor(i)
      Color.RGBtoHSB((rgbB >> 16) & 0xFF, (rgbB >> 8) & 0xFF, rgbB & 0xFF, hsbBot)
      if (vertical) {
        var pixPos = 0
        var off    = i
        while (pixPos < len) {
          val w2 = pixPos * w3
          val w1 = 1.0f - w2
          val rgb = Color.HSBtoRGB(hsbTop(0) * w1 + hsbBot(0) * w2,
            hsbTop(1) * w1 + hsbBot(1) * w2,
            hsbTop(2) * w1 + hsbBot(2) * w2)
          pix(off)     = rgb | 0xFF000000
          pix(off + breadth) = 0xFF000000
          pixPos += 2
          off += (breadth << 1)
        }
      } else {
        var pixPos = 0
        var off = i * len
        while (pixPos < len) {
          val w2 = pixPos * w3
          val w1 = 1.0f - w2
          val rgb = Color.HSBtoRGB(hsbTop(0) * w2 + hsbBot(0) * w1,
            hsbTop(1) * w2 + hsbBot(1) * w1,
            hsbTop(2) * w2 + hsbBot(2) * w1)
          pix(off) = rgb | 0xFF000000
          off     += 1
          pix(off) = 0xFF000000
          off     += 1
          pixPos  += 2
        }
      }
      i += 1
    }

    pix
  }

  private def paintToNorm(paint: Float): Float = {
    if (paint >= -30f) {
      if (paint >= -20f) {
        math.min(1f, paint * 0.025f + 1.0f) // 50 ... 100 %
      } else {
        paint * 0.02f + 0.9f // 30 ... 50 %
      }
    } else if (paint >= -50f) {
      if (paint >= -40f) {
        paint * 0.015f + 0.75f // 15 ... 30 %
      } else {
        paint * 0.01f + 0.55f // 5 ... 15%
      }
    } else if (paint >= -60f) {
      paint * 0.005f + 0.3f // 0 ... 5 %
    } else -1f
  }

  private def currentTime() = System.currentTimeMillis()
}

class PeakMeterBar(orient: Int = SwingConstants.VERTICAL)
  extends JComponent with PeakMeterChannel {

  import PeakMeterBar._
  import SwingConstants._

  private[this] final var holdDurationVar	       = PeakMeter.DefaultHoldDuration   // milliseconds peak hold

  private[this] final val nInf                   = Float.NegativeInfinity
  private[this] final var	peakDB                 = nInf
  private[this] final var rmsDB                  = nInf
  private[this] final var	peakLin                = 0f
  private[this] final var rmsLin                 = 0f
  private[this] final var holdDB                 = nInf
  private[this] final var holdLin                = 0f
  private[this] final var peakToPaint            = 0f
  private[this] final var rmsToPaint             = 0f
  private[this] final var holdToPaint            = 0f
  private[this] final var peakNorm               = 0f
  private[this] final var rmsNorm                = 0f
  private[this] final var holdNorm               = 0f

  private[this] final var recentLength	         = 0
  private[this] final var recentBreadth	         = 0
  private[this] final var calculatedLength	     = -1			// recentHeight snapshot in recalculatePaint()
  private[this] final var calculatedBreadth	     = -1			// recentWidth snapshot in recalculatePaint()
  private[this] final var lastUpdate		         = currentTime()
  private[this] final var holdEnd                = 0L

  private[this] final var holdPaintedVar		     = true
  private[this] final var rmsPaintedVar		       = true

  private[this] final var pntBg: Paint			     = null
  private[this] final var imgBg  : BufferedImage = null
  private[this] final var imgRMS : BufferedImage = null
  private[this] final var imgPeak: BufferedImage = null

  private[this] final val ins                    = new Insets(0, 0, 0, 0)

  private[this] final var holdPixPos             = 0
  private[this] final var peakPixPos             = 0
  private[this] final var rmsPixPos              = 0

  private[this] final var peakPixPosP	           = 0
  private[this] final var rmsPixPosP	           = 0
  private[this] final var holdPixPosP	           = 0

  var refreshParent                        = false

  private[this] final var ticksVar = 101
  private[this] final var vertical = {
    val res = orient == VERTICAL
    if (!res && orient != HORIZONTAL) throw new IllegalArgumentException(orient.toString)
    res
  }

  // ---- constructor ----
  setOpaque(true)
  setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1))
  recalculatePrefSize()
  addPropertyChangeListener("border", new PropertyChangeListener {
    def propertyChange(e: PropertyChangeEvent): Unit = recalculatePrefSize()
  })
  clearMeter()

   // ---- ----------- ----

  def orientation_=(orient: Int): Unit = {
    val newVertical = orient == VERTICAL
    if (orient != HORIZONTAL && !newVertical) throw new IllegalArgumentException(orient.toString)

    if (newVertical != vertical) {
      vertical = newVertical
      disposeImages()
      recalculatePrefSize()
      clearMeter()
    }
  }

  def orientation: Int = if (vertical) VERTICAL else HORIZONTAL

  // ------------- PeakMeterView interface -------------

   /** Decides whether the peak indicator should be
     *	painted or not. By default the indicator is painted.
     *
     *	@param	b	<code>true</code> to have the indicator painted,
     *					<code>false</code> to switch it off
     */
   def holdPainted_=(b: Boolean): Unit = if (holdPaintedVar != b) {
     holdPaintedVar = b
     repaint()
   }

  def holdPainted: Boolean = holdPaintedVar

  /** Decides whether the blue RMS bar should be
    *	painted or not. By default the bar is painted.
    *
    *	@param	b	<code>true</code> to have the RMS values painted,
    *					<code>false</code> to switch them off
    */
  def rmsPainted_=(b: Boolean): Unit = if (rmsPaintedVar != b) {
    rmsPaintedVar = b
    repaint()
  }

  def rmsPainted: Boolean = rmsPaintedVar

  /** Clears the peak, peak hold and rms values
    *	immediately (without ballistics). This
    *	way the component can be reset when the
    *	metering task is stopped without waiting
    *	for the bars to fall down.
    */
  def clearMeter(): Unit = {
    val w1      = getWidth  - (ins.left + ins.right )
    val h1      = getHeight - (ins.top  + ins.bottom)
    val len1    = if (vertical) h1 else w1
    val rLen1   = (len1 - 1) & ~1

    peakDB		  = nInf
    rmsDB		    = nInf
    holdDB	    = nInf
    peakLin     = 0f
    rmsLin      = 0f
    holdLin     = 0f
    peakToPaint	= nInf
    rmsToPaint	= nInf
    holdToPaint = nInf
    peakNorm		= -1f
    rmsNorm		  = -1f
    holdNorm		= -1f
    holdEnd		  = currentTime()

    holdPixPos	= (holdNorm * rLen1).toInt & ~1
    peakPixPos	= (peakNorm * rLen1).toInt & ~1
    rmsPixPos   = math.min((rmsNorm * rLen1).toInt & ~1, peakPixPos - 4)

    if (refreshParent) {
      getParent.repaint(ins.left + getX, ins.top + getY, w1, h1)
    } else {
      repaint(ins.left, ins.top, w1, h1)
    }
  }

  // ----------- public methods -----------

  def ticks_=(num: Int): Unit = if (ticksVar != num) {
    ticksVar = num
    recalculatePrefSize()
  }

  def ticks: Int = ticksVar

  /** Sets the peak indicator hold time. Defaults to 1800 milliseconds.
    *
    *	@param	millis	new peak hold time in milliseconds. Note that
    *					you can use `Int.MaxValue` for an infinite
    *					peak hold. In this case, to clear the indicator,
    *					call <code>clearHold</code>
    */
  def holdDuration_=(millis: Int): Unit = {
    holdDurationVar = millis
    holdEnd         = currentTime()
  }

  def holdDuration: Int = holdDurationVar

  /** Clears the peak hold
    *	indicator. Note that you will need
    *	to call <code>setPeakAndRMS</code> successively
    *	for the graphics to be updated.
    */
  def clearHold(): Unit = {
    holdDB = nInf
    holdLin = 0f
    holdNorm = 0f
  }

  protected def recalculatePrefSize(): Unit = {
    var minDim : Dimension = null
    var prefDim: Dimension = null
    getInsets(ins)
    if (vertical) {
      val w   = 10 + ins.left + ins.right
      minDim  = new Dimension(4, 2 + ins.top + ins.bottom)
      prefDim = new Dimension(w, if (ticksVar <= 0) getPreferredSize.height else ticksVar * 2 - 1 + ins.top + ins.bottom)
    } else {
      val h   = 10 + ins.top + ins.bottom
      minDim  = new Dimension(2 + ins.left + ins.right, 4)
      prefDim = new Dimension(if (ticksVar <= 0) getPreferredSize.width else ticksVar * 2 - 1 + ins.left + ins.right, h)
    }
    setMinimumSize(minDim)
    setPreferredSize(prefDim)
  }

  def peakDecibels: Float = peakDB
  def rmsDecibels : Float = rmsDB
  def holdDecibels: Float = holdDB

  def peak: Float = peakLin

  def peak_=(value: Float): Unit = {
    peakLin = value
    peakDB  = (math.log(value) * logPeakCorr).toFloat
    triggerRefresh(currentTime())
  }

  def rms: Float = rmsLin

  def rms_=(value: Float): Unit = {
    rmsLin = value
    rmsDB  = (math.log(value) * logRMSCorr).toFloat
    if (rmsPaintedVar) triggerRefresh(currentTime())
  }

  def hold: Float = holdLin

  def hold_=(value: Float): Unit = {
    holdLin = value
    holdDB  = (math.log(value) * logPeakCorr).toFloat
    if (holdPaintedVar) triggerRefresh(currentTime())
  }

  /** Updates the meter. This will call the component's paint
    *	method to visually reflect the new values. Call this method
    *	regularly for a steady animated meter.
    *	<p>
    *	If you have switched off RMS painted, you may want to
    *	call <code>setPeak</code> alternatively.
    *	<p>
    *	When your audio engine is idle, you may want to stop meter updates.
    *	You can use the following formula to calculate the maximum delay
    *	of the meter display to be safely at minimum levels after starting
    *	to send zero amplitudes:
    *	</p><UL>
    *	<LI>for peak hold indicator not painted : delay[sec] = abs(minAmplitude[dB]) / fallTime[dB/sec]
    *	+ updatePeriod[sec]</LI>
    *	<LI>for painted peak hold : the maximum of the above value and
    *	delay[sec] = abs(minAmplitude[dB]) / holdFallTime[dB/sec] + holdTime[sec] + updatePeriod[sec]
    *	</LI>
    *	</UL><P>
    *	Therefore, for the default values of 1.8 sec hold time, 15 dB/sec hold fall time and -40 dB
    *	minimum amplitude, at a display period of 30 milliseconds, this yields a
    *	delay of around 4.5 seconds. Accounting for jitter due to GUI slowdown, in ths case it should be
    *	safe to stop meter updates five seconds after the audio engine stopped.
    *
    *	@param	newPeak	peak amplitude (linear) between zero and one.
    *	@param	newRMS		mean-square amplitude (linear). note : despite the name,
    *					this is considered mean-square, not root-mean-square. this
    *					method does the appropriate conversion on the fly!
    */
  def update(newPeak: Float, newRMS: Float, time: Long): Boolean = {
    //      if( !EventQueue.isDispatchThread ) throw new IllegalMonitorStateException()

    val newPeakDB = (math.log( newPeak ) * logPeakCorr).toFloat
    if (newPeakDB >= peakDB) {
      peakLin = newPeak
      peakDB  = newPeakDB
    } else {
      // 20 dB in 1500 ms bzw. 40 dB in 2500 ms
      peakDB  = math.max(newPeakDB, peakDB - (time - lastUpdate) * (if (peakDB > -20f) 0.013333333333333f else 0.016f))
      peakLin = math.exp(peakDB / logPeakCorr).toFloat
    }

    val newRmsDb = (math.log(newRMS) * logRMSCorr).toFloat
    if (newRmsDb > rmsDB) {
      rmsLin = newRMS
      rmsDB  = newRmsDb
    } else {
      rmsDB  = math.max(newRmsDb, rmsDB - (time - lastUpdate) * (if (rmsDB > -20f) 0.013333333333333f else 0.016f))
    }

    if (peakDB >= holdDB) {
      holdDB  = peakDB
      holdLin = peakLin
      holdEnd = time + holdDurationVar
    } else if (time > holdEnd) {
      if (peakDB > holdDB) {
        holdDB  = peakDB
        holdLin = peakLin
      } else {
        holdDB += (if (holdDB > -20f) 0.013333333333333f else 0.016f) * (lastUpdate - time)
        holdLin = math.exp(holdDB / logPeakCorr).toFloat
      }
    }

    triggerRefresh(time)
  }

  private def triggerRefresh(time: Long): Boolean = {
    peakToPaint  = math.max(peakToPaint, peakDB)
    peakNorm     = paintToNorm(peakToPaint)
    if (rmsPaintedVar) {
      rmsToPaint = math.max(rmsToPaint, rmsDB)
      rmsNorm    = paintToNorm(rmsToPaint)
    }

    val result    = if (holdPaintedVar) {
      holdToPaint = math.max(holdToPaint, holdDB)
      holdNorm    = paintToNorm(holdToPaint)
      holdNorm   >= 0f
    } else {
      peakNorm   >= 0f
    }

    lastUpdate    = time
    val w1        = getWidth - ins.left - ins.right
    val h1        = getHeight - ins.top - ins.bottom
    val len1      = if (vertical) h1 else w1
    val rLen1     = (len1 - 1) & ~1
    recentLength  = rLen1 + 1

    holdPixPos    = (holdNorm * rLen1).toInt & ~1
    peakPixPos    = (peakNorm * rLen1).toInt & ~1
    rmsPixPos     = math.min((rmsNorm * rLen1).toInt & ~1, peakPixPos - 4)

    // repaint only if pixel coordinates changed
    val peakPixChanged  = peakPixPos != peakPixPosP
    val rmsPixChanged   = rmsPixPos  != rmsPixPosP
    val holdPixChanged  = holdPixPos != holdPixPosP

    if (peakPixChanged || rmsPixChanged || holdPixChanged) {
      var minPixPos = 0
      var maxPixPos = 0

      // calculate dirty span
      if (peakPixPos < peakPixPosP) {
        minPixPos = peakPixPos
        maxPixPos = peakPixPosP
      } else {
        minPixPos = peakPixPosP
        maxPixPos = peakPixPos
         }
      if (holdPaintedVar) {
        if (holdPixPos < holdPixPosP) {
          if (holdPixPos < minPixPos) minPixPos = holdPixPos
          if (holdPixPosP > maxPixPos) maxPixPos = holdPixPosP
        } else {
          if (holdPixPosP < minPixPos) minPixPos = holdPixPosP
          if (holdPixPos > maxPixPos) maxPixPos = holdPixPos
        }
      }
      if (rmsPaintedVar) {
        if (rmsPixPos < rmsPixPosP) {
          if (rmsPixPos < minPixPos) minPixPos = rmsPixPos
          if (rmsPixPosP > maxPixPos) maxPixPos = rmsPixPosP
        } else {
          if (rmsPixPosP < minPixPos) minPixPos = rmsPixPosP
          if (rmsPixPos > maxPixPos) maxPixPos = rmsPixPos
        }
      }

      var c: Container = null
      var offX = 0
      var offY = 0

      if (refreshParent) {
        c = getParent
        offX = ins.left + getX
        offY = ins.top + getY
      } else {
        c = this
        offX = ins.left
        offY = ins.top
      }

      // trigger repaint
      if (vertical) {
        c.repaint(offX, offY + rLen1 - maxPixPos, w1, maxPixPos - minPixPos + 2)
      } else {
        c.repaint(offX + minPixPos, offY, maxPixPos - minPixPos + 2, h1)
      }
      if (Util.needsSync) getToolkit.sync()

    } else {
      peakToPaint = nInf
      rmsToPaint  = nInf
      holdToPaint = nInf
    }

    result
  }

  private def recalculatePaint(): Unit = {
    val imgLen		  = (recentLength + 1) & ~1
    val imgBreadth  = recentBreadth
    var imgW        = 0
    var imgH        = 0

    if (imgPeak != null) {
      imgPeak.flush()
      imgPeak = null
    }
    if (imgRMS != null) {
      imgRMS.flush()
      imgRMS = null
    }

    if (vertical) {
      // ---- vertical ----
      if ((imgBg == null) || (imgBg.getWidth != imgBreadth)) {
        if (imgBg != null) {
          imgBg.flush()
          imgBg = null
        }
        val pix = if (imgBreadth == 10) {
          bgPixels
        } else {
          widenPixV(bgPixels, 10, imgBreadth, 2)
        }
        imgBg = new BufferedImage(imgBreadth, 2, BufferedImage.TYPE_INT_ARGB)
        imgBg.setRGB(0, 0, imgBreadth, 2, pix, 0, imgBreadth)
        pntBg = new TexturePaint(imgBg, new Rectangle(0, 0, imgBreadth, 2))
      }
      imgW = imgBreadth
      imgH = imgLen

    } else {
      // ---- horizontal ----
      if ((imgBg == null) || (imgBg.getHeight != imgBreadth)) {
        if (imgBg != null) {
          imgBg.flush()
          imgBg = null
        }
        val pix = widenPixH(bgPixels, 10, imgBreadth, 2)
        imgBg   = new BufferedImage(2, imgBreadth, BufferedImage.TYPE_INT_ARGB)
        imgBg.setRGB(0, 0, 2, imgBreadth, pix, 0, 2)
        pntBg   = new TexturePaint(imgBg, new Rectangle(0, 0, 2, imgBreadth))
      }
      imgW = imgLen
      imgH = imgBreadth
    }
    val pix1 = hsbFade(imgBreadth, imgLen, rmsTopColor, rmsBotColor, vertical)
    imgRMS   = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB)
    imgRMS.setRGB(0, 0, imgW, imgH, pix1, 0, imgW)

    val pix2 = hsbFade(imgBreadth, imgLen, peakTopColor, peakBotColor, vertical)
    imgPeak  = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB)
    imgPeak.setRGB(0, 0, imgW, imgH, pix2, 0, imgW)

    calculatedLength  = recentLength
    calculatedBreadth = recentBreadth
  }

  override def paintComponent(g: Graphics): Unit = {
    super.paintComponent(g)

    val w   = getWidth
    val h   = getHeight
    val w1  = w - (ins.left + ins.right)
    val h1  = h - (ins.top + ins.bottom)

    val len1 = if (vertical) {
      recentBreadth = w1
      h1
    } else {
      recentBreadth = h1
      w1
    }
    val rLen1 = (len1 - 1) & ~1
    val len   = rLen1 + 1

    g.setColor(Color.black)
    g.fillRect(0, 0, w, h)
    if (len <= 0) return

    if (len != recentLength) {
      holdPixPos   = (holdNorm * rLen1).toInt & ~1
      peakPixPos   = (peakNorm * rLen1).toInt & ~1
      rmsPixPos    = math.min((rmsNorm * rLen1).toInt & ~1, peakPixPos - 4)
      recentLength = len
    }
    if ((calculatedLength != recentLength) || (calculatedBreadth != recentBreadth)) {
      recalculatePaint()
    }

    val g2 = g.asInstanceOf[Graphics2D]
    val atOrig = g2.getTransform

    if (vertical) {
      // ---- vertical ----
      g2.translate(ins.left, ins.top + (len1 - len))
      g2.setPaint(pntBg)
      val holdPixPosI = rLen1 - holdPixPos
      val peakPixPosI = rLen1 - peakPixPos
      if (rmsPaintedVar) {
        val rmsPixPosI = rLen1 - rmsPixPos
        g2.fillRect(0, 0, recentBreadth, math.min(len, rmsPixPosI))
        if (holdPaintedVar && (holdPixPos >= 0)) {
          g2.drawImage(imgPeak, 0, holdPixPosI, recentBreadth, holdPixPosI + 1,
            0, holdPixPosI, recentBreadth, holdPixPosI + 1, this)
        }
        if (peakPixPos >= 0) {
          val lenClip = math.min(len, rmsPixPosI - 2)
          g2.drawImage(imgPeak, 0, peakPixPosI, recentBreadth, lenClip,
            0, peakPixPosI, recentBreadth, lenClip, this)
        }
        if (rmsPixPos >= 0) {
          g2.drawImage(imgRMS, 0, rmsPixPosI, recentBreadth, len,
            0, rmsPixPosI, recentBreadth, len, this)
        }
      } else {
        g2.fillRect(0, 0, recentBreadth, peakPixPosI)
        if (holdPaintedVar && (holdPixPos >= 0)) {
          g2.drawImage(imgPeak, 0, holdPixPosI, recentBreadth, holdPixPosI + 1,
            0, holdPixPosI, recentBreadth, holdPixPosI + 1, this)
        }
        if (peakPixPos >= 0) {
          g2.drawImage(imgPeak, 0, peakPixPosI, recentBreadth, len,
            0, peakPixPosI, recentBreadth, len, this)
        }
      }
    } else {
      // ---- horizontal ----
      g2.translate(ins.left, ins.top)
      g2.setPaint(pntBg)
      if (rmsPaintedVar) {
        val rmsPixPosC = math.max(0, rmsPixPos)
        g2.fillRect(rmsPixPosC, 0, len - rmsPixPosC, recentBreadth)
        if (holdPaintedVar && (holdPixPos >= 0)) {
          g2.drawImage(imgPeak, holdPixPos, 0, holdPixPos + 1, recentBreadth,
            holdPixPos, 0, holdPixPos + 1, recentBreadth, this)
        }
        if (peakPixPos >= 0) {
          val offClip = math.max(0, rmsPixPos + 3)
          g2.drawImage(imgPeak, offClip, 0, peakPixPos + 1, recentBreadth,
            offClip, 0, peakPixPos + 1, recentBreadth, this)
        }
        if (rmsPixPos >= 0) {
          g2.drawImage(imgRMS, 0, 0, rmsPixPos + 1, recentBreadth,
            0, 0, rmsPixPos + 1, recentBreadth, this)
        }
      } else {
        val peakPixPosC = math.max(0, peakPixPos)
        g2.fillRect(peakPixPosC, 0, len - peakPixPosC, recentBreadth)
        if (holdPaintedVar && (holdPixPos >= 0)) {
          g2.drawImage(imgPeak, holdPixPos, 0, holdPixPos + 1, recentBreadth,
            holdPixPos, 0, holdPixPos + 1, recentBreadth, this)
        }
        if (peakPixPos >= 0) {
          g2.drawImage(imgPeak, 0, 0, peakPixPos + 1, recentBreadth,
            0, 0, peakPixPos + 1, recentBreadth, this)
        }
      }
    }

    peakToPaint = nInf
    rmsToPaint  = nInf
    holdToPaint = nInf
    peakPixPosP = peakPixPos
    rmsPixPosP  = rmsPixPos
    holdPixPosP = holdPixPos

    g2.setTransform(atOrig)
  }

   // --------------- Disposable interface ---------------

  private def disposeImages(): Unit = {
    if (imgPeak != null) {
      imgPeak.flush()
      imgPeak = null
    }
    if (imgRMS != null) {
      imgRMS.flush()
      imgRMS = null
    }
    if (imgBg != null) {
      imgBg.flush()
      imgBg = null
      pntBg = null
    }
    calculatedLength = -1
  }

  def dispose(): Unit = disposeImages()
}