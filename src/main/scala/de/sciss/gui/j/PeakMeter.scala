/*
 *  PeakMeter.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License
 *	as published by the Free Software Foundation; either
 *	version 2, june 1991 of the License, or (at your option) any later version.
 *
 *	This software is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License (gpl.txt) along with this software; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.gui.j

import java.beans.{PropertyChangeListener, PropertyChangeEvent}
import javax.swing.{BorderFactory, JComponent}
import java.awt.image.BufferedImage
import java.awt.{Color, Rectangle, TexturePaint, Graphics2D, Graphics, Container, EventQueue, Adjustable, Paint, Dimension, Insets}

trait PeakMeterLike {
   def clearHold() : Unit
   def clearMeter() : Unit
   def dispose(): Unit
   def holdDecibels : Float
   def holdDuration : Int
   def holdDuration_=( millis: Int ) : Unit
   def holdPainted : Boolean
   def holdPainted_=( b: Boolean ): Unit
//   def orientation : Int
//   def orientation_=( orient: Int ): Unit
   def peak : Float
   def peak_=( value: Float ) : Unit
   def peakDecibels : Float
   var refreshParent : Boolean
   def rms : Float
   def rms_=( value: Float ) : Unit
   def rmsPainted : Boolean
   def rmsPainted_=( b: Boolean ) : Unit
   def ticks : Int
   def ticks_= (num: Int): Unit
   def update( peak: Float, rms: Float = rms, time: Long = System.currentTimeMillis ) : Boolean
}

/**
 *	A level (volume) meter GUI component. The component
 *	is a vertical bar displaying a green-to-reddish bar
 *	for the peak amplitude and a blue bar for RMS value.
 *	<p>
 *	To animate the bar, call <code>setPeakAndRMS</code> at a
 *	regular interval, typically around every 30 milliseconds
 *	for a smooth look.
 *
 *	@todo	allow linear display (now it's hard coded logarithmic)
 *	@todo	add optional horizontal orientation
 */
object PeakMeter {
   private val DEFAULT_HOLD_DUR  = 2500
   private val logPeakCorr		   = 20.0 / math.log( 10 )
   private val logRMSCorr		   = 10.0 / math.log( 10 )

   private val bgPixels		= Array( 0xFF000000, 0xFF343434, 0xFF484848, 0xFF5C5C5C, 0xFF5C5C5C,
                                    0xFF5C5C5C, 0xFF5C5C5C, 0xFF5C5C5C, 0xFF484848, 0xFF343434,
                                    0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000,
                                    0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000 )

   private val rmsTopColor = Array( 0x000068, 0x5537B9, 0x764EE5, 0x9062E8, 0x8B63E8,
                                    0x8360E8, 0x7C60E8, 0x8876EB, 0x594CB4, 0x403A63 )

   private val rmsBotColor	= Array( 0x000068, 0x2F4BB6, 0x4367E2, 0x577FE5, 0x577AE5,
                                    0x5874E6, 0x596FE6, 0x6B7AEA, 0x4851B1, 0x393D62 )

   private val peakTopColor= Array( 0x000000, 0xB72929, 0xFF3C3C, 0xFF6B6B, 0xFF6B6B,
                                    0xFF6B6B, 0xFF6B6B, 0xFFA7A7, 0xFF3C3C, 0xB72929 )

   private val peakBotColor= Array( 0x000000, 0x008E00, 0x00C800, 0x02FF02, 0x02FF02,
                                    0x02FF02, 0x02FF02, 0x68FF68, 0x00C800, 0x008E00 )

   private def widenPixV( src: Array[ Int ], srcBrdth: Int, dstBrdth: Int, len: Int ) : Array[ Int ] = {
      val minBrdth	= math.min( srcBrdth, dstBrdth )
      val minBrdthH	= minBrdth >> 1
      val minBrdthH1	= minBrdth - minBrdthH
      val numWiden	= dstBrdth - srcBrdth
      val dst			= new Array[ Int ]( dstBrdth * len )

      var y       = 0
      var srcOffL = 0
      var srcOffR = srcBrdth - minBrdthH1
      var dstOffL = 0
      var dstOffR = dstBrdth - minBrdthH1
      while( y < len ) {
         System.arraycopy( src, srcOffL, dst, dstOffL, minBrdthH )
         System.arraycopy( src, srcOffR, dst, dstOffR, minBrdthH1 )
         y       += 1
         srcOffL += srcBrdth
         srcOffR += srcBrdth
         dstOffL += dstBrdth
         dstOffR += dstBrdth
      }
      if( numWiden > 0 ) {
         y        = 0
         srcOffL  = minBrdthH
         dstOffL  = minBrdthH
         while( y < len ) {
            val p = src( srcOffL )
            dstOffR = dstOffL + numWiden
            while( dstOffL < dstOffR ) {
               dst( dstOffL ) = p
               dstOffL += 1
            }
            y       += 1
            srcOffL += srcBrdth
            dstOffL += srcBrdth
         }
      }
      dst
   }

   private def widenPixH( src: Array[ Int ], srcBrdth: Int, dstBrdth: Int, len: Int ) : Array[ Int ] = {
      val minBrdth	= math.min( srcBrdth, dstBrdth )
      val minBrdthH	= minBrdth >> 1
      val minBrdthH1	= minBrdth - minBrdthH
      val brdthDOff	= dstBrdth - minBrdthH1
      val brdthSOff	= srcBrdth - minBrdthH1
      val dst			= new Array[ Int ]( dstBrdth * len )

      var dstOff  = 0
      var y       = 0
      while( y < minBrdthH ) {
         var x       = 0
         var srcOff  = y
         while( x < len ) {
            dst( dstOff ) = src( srcOff )
            x      += 1
            dstOff += 1
            srcOff += srcBrdth
         }
         y += 1
      }
      while( y < brdthDOff ) {
         var x       = 0
         var srcOff  = minBrdthH
         while( x < len ) {
            dst( dstOff ) = src( srcOff )
            x      += 1
            dstOff += 1
            srcOff += srcBrdth
         }
         y += 1
      }
      var srcOffS = brdthSOff
      while( y < dstBrdth ) {
         var x       = 0
         var srcOff  = srcOffS
         while( x < len ) {
            dst( dstOff ) = src( srcOff )
            x      += 1
            dstOff += 1
            srcOff += srcBrdth
         }
         y        += 1
         srcOffS  += 1
      }
      dst
   }

   private def hsbFade( brdth: Int, len: Int, topColr: Array[ Int ], botColr: Array[ Int ], vertical: Boolean ) : Array[ Int ] = {
      val pix        = new Array[ Int ]( brdth * len )
      val hsbTop     = new Array[ Float ]( 3 )
      val hsbBot     = new Array[ Float ]( 3 )
      val w3         = 1.0f / (len - 2)
      val best       = brdth == 10
      val sTopColr   = if( best ) topColr else widenPixV( topColr, 10, brdth, 1 )
      val sBotColr   = if( best ) botColr else widenPixV( botColr, 10, brdth, 1 )

      var i = 0
      while( i < brdth ) {
         val rgbT = sTopColr( i )
         Color.RGBtoHSB( (rgbT >> 16) & 0xFF, (rgbT >> 8) & 0xFF, rgbT & 0xFF, hsbTop )
         val rgbB = sBotColr( i )
         Color.RGBtoHSB( (rgbB >> 16) & 0xFF, (rgbB >> 8) & 0xFF, rgbB & 0xFF, hsbBot )
         if( vertical ) {
            var pixPos  = 0
            var off     = i
            while( pixPos < len ) {
               val w2   = pixPos * w3
               val w1   = 1.0f - w2
               val rgb  = Color.HSBtoRGB( hsbTop(0) * w1 + hsbBot(0) * w2,
                                hsbTop(1) * w1 + hsbBot(1) * w2,
                                hsbTop(2) * w1 + hsbBot(2) * w2 )
               pix( off ) = rgb | 0xFF000000
               pix( off+brdth ) = 0xFF000000
               pixPos += 2
               off    += (brdth << 1)
            }
         } else {
            var pixPos  = 0
            var off     = i * len
            while( pixPos < len ) {
               val w2   = pixPos * w3
               val w1   = 1.0f - w2
               val rgb  = Color.HSBtoRGB( hsbTop(0) * w2 + hsbBot(0) * w1,
                                hsbTop(1) * w2 + hsbBot(1) * w1,
                                hsbTop(2) * w2 + hsbBot(2) * w1 )
               pix( off )  = rgb | 0xFF000000
               off        += 1
               pix( off )  = 0xFF000000
               off        += 1
               pixPos     += 2
            }
         }
         i += 1
      }

      pix
   }

   private def paintToNorm( paint: Float ) : Float = {
      if( paint >= -30f ) {
         if( paint >= -20f ) {
            math.min( 1f, paint * 0.025f + 1.0f )  // 50 ... 100 %
         } else {
            paint * 0.02f + 0.9f   // 30 ... 50 %
         }
      } else if( paint >= -50f ) {
         if( paint >= -40f ) {
            paint * 0.015f + 0.75f 	// 15 ... 30 %
         } else {
            paint * 0.01f + 0.55f 	// 5 ... 15%
         }
      } else if( paint >= -60f ) {
         paint * 0.005f + 0.3f 	// 0 ... 5 %
      } else -1f
   }
}

class PeakMeter( orient: Int = Adjustable.VERTICAL ) extends JComponent with PeakMeterLike /* with PeakMeterView */ {
   import PeakMeter._

   private var holdDurationVar	      = DEFAULT_HOLD_DUR   // milliseconds peak hold

   private var	peakVar                 = 0f
   private var rmsVar                  = 0f
   private var hold                    = 0f
   private var peakToPaint             = 0f
   private var rmsToPaint              = 0f
   private var holdToPaint             = 0f
   private var peakNorm                = 0f
   private var rmsNorm                 = 0f
   private var holdNorm                = 0f

   private var recentLength	         = 0
   private var recentBreadth	         = 0
   private var calcedLength	         = -1			// recentHeight snapshot in recalcPaint()
   private var calcedBreadth	         = -1			// recentWidth snapshot in recalcPaint()
   private var lastUpdate		         = System.currentTimeMillis
   private var holdEnd                 = 0L

   private var holdPaintedVar		      = true
   private var rmsPaintedVar		      = true

   private var pntBg: Paint			   = null
   private var imgBg: BufferedImage    = null
   private var imgRMS: BufferedImage   = null
   private var imgPeak: BufferedImage  = null

   private val ins                     = new Insets( 0, 0, 0, 0 )

   private var holdPixPos              = 0
   private var peakPixPos              = 0
   private var rmsPixPos               = 0

   private var peakPixPosP	            = 0
   private var rmsPixPosP	            = 0
   private var holdPixPosP	            = 0

   var refreshParent                   = false

   private var ticksVar		            = 0
   private var vertical = {
      val res = orient == Adjustable.VERTICAL
      if( !res && orient != Adjustable.HORIZONTAL ) throw new IllegalArgumentException( orient.toString )
      res
   }

   // ---- constructor ----
   setOpaque( true )
   setBorder( BorderFactory.createEmptyBorder( 1, 1, 1, 1 ))
   recalcPrefSize()
   addPropertyChangeListener( "border", new PropertyChangeListener {
      def propertyChange( e: PropertyChangeEvent ) { recalcPrefSize() }
   })
   clearMeter()

   // ---- ----------- ----

   def orientation_=( orient: Int ) {
      val newVertical = orient == Adjustable.VERTICAL
      if( orient != Adjustable.HORIZONTAL && !newVertical ) {
         throw new IllegalArgumentException( orient.toString )
      }
      if( newVertical != vertical ) {
         vertical = newVertical
         disposeImages()
         recalcPrefSize()
         clearMeter()
      }
   }
   def orientation: Int = if( vertical ) Adjustable.VERTICAL else Adjustable.HORIZONTAL

   // ------------- PeakMeterView interface -------------

//   def numChannels = 1
//
//   def meterUpdate( peakRMSPairs: Array[ Float ], offset: Int, time: Long ) : Boolean = {
//      val offset2 = offset + 1
//      if( offset2 < peakRMSPairs.length ) {
//         setPeakAndRMS( peakRMSPairs( offset ), peakRMSPairs( offset2 ), time )
//      } else false
//   }

   /**
    *	Decides whether the peak indicator should be
    *	painted or not. By default the indicator is painted.
    *
    *	@param	onOff	<code>true</code> to have the indicator painted,
    *					<code>false</code> to switch it off
    */
   def holdPainted_=( b: Boolean ) {
      if( holdPaintedVar != b ) {
         holdPaintedVar	= b
         repaint()
      }
   }
   def holdPainted : Boolean = holdPaintedVar

   /**
    *	Decides whether the blue RMS bar should be
    *	painted or not. By default the bar is painted.
    *
    *	@param	onOff	<code>true</code> to have the RMS values painted,
    *					<code>false</code> to switch them off
    */
   def rmsPainted_=( b: Boolean ) {
      if( rmsPaintedVar != b ) {
         rmsPaintedVar= b
         repaint()
      }
   }
   def rmsPainted : Boolean = rmsPaintedVar

   /**
    *	Clears the peak, peak hold and rms values
    *	immediately (without ballistics). This
    *	way the component can be reset when the
    *	metering task is stopped without waiting
    *	for the bars to fall down.
    */
   def clearMeter() {
      val w1		= getWidth  - (ins.left + ins.right)
      val h1		= getHeight - (ins.top + ins.bottom)
      val len1	   = if( vertical ) h1 else w1
      val rlen1	= (len1 - 1) & ~1

      peakVar			= -160f
      rmsVar		   = -160f
      hold			= -160f
      peakToPaint	= -160f
      rmsToPaint	= -160f
      holdToPaint = -160f
      peakNorm		= -1.0f
      rmsNorm		= -1.0f
      holdNorm		= -1.0f
      holdEnd		= System.currentTimeMillis

      holdPixPos	= (holdNorm * rlen1).toInt & ~1
      peakPixPos	= (peakNorm * rlen1).toInt & ~1
      rmsPixPos	= math.min( (rmsNorm  * rlen1).toInt & ~1, peakPixPos - 4 )

      if( refreshParent ) {
         getParent.repaint( ins.left + getX, ins.top + getY, w1, h1 )
      } else {
         repaint( ins.left, ins.top, w1, h1 )
      }
   }

   // ----------- public methods -----------

   def ticks_=( num: Int ) {
      if( ticksVar != num ) {
         ticksVar = num
         recalcPrefSize()
      }
   }
   def ticks : Int = ticksVar

   /**
    *	Sets the peak indicator hold time. Defaults to 1800 milliseconds.
    *
    *	@param	millis	new peak hold time in milliseconds. Note that
    *					you can use `Int.MaxValue` for an infinite
    *					peak hold. In this case, to clear the indicator,
    *					call <code>clearHold</code>
    */
   def holdDuration_=( millis: Int ) {
      holdDurationVar	= millis
      holdEnd			   = System.currentTimeMillis
   }
   def holdDuration : Int = holdDurationVar

   /**
    *	Clears the peak hold
    *	indicator. Note that you will need
    *	to call <code>setPeakAndRMS</code> successively
    *	for the graphics to be updated.
    */
   def clearHold() {
      hold		= -160f
      holdNorm	= 0.0f
   }

   protected def recalcPrefSize() {
      var minDim: Dimension   = null
      var prefDim: Dimension  = null
      getInsets( ins )
      if( vertical ) {
         val w = 10 + ins.left + ins.right
         minDim  = new Dimension( 4, 2 + ins.top + ins.bottom )
         prefDim = new Dimension( w, if( ticksVar <= 0 ) getPreferredSize.height else (ticksVar * 2 - 1 + ins.top + ins.bottom) )
      } else {
         val h = 10 + ins.top + ins.bottom
         minDim  = new Dimension( 2 + ins.left + ins.right, 4 )
         prefDim = new Dimension( if( ticksVar <= 0 ) getPreferredSize.width else (ticksVar * 2 - 1 + ins.left + ins.right), h )
      }
      setMinimumSize( minDim )
      setPreferredSize( prefDim )
   }

   def peakDecibels : Float = if( peakVar <= -160f ) Float.NegativeInfinity else peakVar
   def holdDecibels : Float = if( hold <= -160f ) Float.NegativeInfinity else hold

   def peak : Float = peakVar
   def peak_=( value: Float ) : Unit = update( value, rmsVar, System.currentTimeMillis )

   def rms : Float = rmsVar
   def rms_=( value: Float ) : Unit = update( peakVar, value, System.currentTimeMillis )

   /**
    *	Updates the meter. This will call the component's paint
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
    *	@param	peak	peak amplitude (linear) between zero and one.
    *	@param	rms		mean-square amplitude (linear). note : despite the name,
    *					this is considered mean-square, not root-mean-square. this
    *					method does the appropriate conversion on the fly!
    *
    *	@synchronization	this method is thread safe
    */
   def update( newPeak: Float, newRMS: Float, time: Long ) : Boolean = {
      if( !EventQueue.isDispatchThread ) throw new IllegalMonitorStateException()

      val newPeak0 = (math.log( newPeak ) * logPeakCorr).toFloat
      peakVar = if( newPeak0 >= peakVar ) {
         newPeak0
      } else {
         // 20 dB in 1500 ms bzw. 40 dB in 2500 ms
         math.max( newPeak0, peakVar - (time - lastUpdate) * (if( peakVar > -20f ) 0.013333333333333f else 0.016f) )
      }
      peakToPaint	= math.max( peakToPaint, peakVar )
      peakNorm 	= paintToNorm( peakToPaint )

      if( rmsPaintedVar ) {
         val newRMS0 = (math.log( newRMS ) * logRMSCorr).toFloat
         rmsVar = if( newRMS0 > rmsVar ) {
            newRMS0
         } else {
            math.max( newRMS0, rmsVar - (time - lastUpdate) * (if( rmsVar > -20f ) 0.013333333333333f else 0.016f) )
         }
         rmsToPaint	= math.max( rmsToPaint, rmsVar )
         rmsNorm		= paintToNorm( rmsToPaint )
      }

      val result = if( holdPaintedVar ) {
         if( peakVar >= hold ) {
            hold     = peakVar
            holdEnd  = time + holdDurationVar
         } else if( time > holdEnd ) {
            if( peakVar > hold ) {
               hold	= peakVar
            } else {
               hold += (if( hold > -20f ) 0.013333333333333f else 0.016f) * (lastUpdate - time)
            }
         }
         holdToPaint = math.max( holdToPaint, hold )
         holdNorm    = paintToNorm( holdToPaint )
         holdNorm >= 0f
      } else {
         peakNorm >= 0f
      }

      lastUpdate     = time
      val w1         = getWidth - ins.left - ins.right
      val h1         = getHeight - ins.top - ins.bottom
      val len1	      = if( vertical ) h1 else w1
      val rlen1      = (len1 - 1) & ~1
      recentLength   = rlen1 + 1

      holdPixPos		= (holdNorm * rlen1).toInt & ~1
      peakPixPos		= (peakNorm * rlen1).toInt & ~1
      rmsPixPos		= math.min( (rmsNorm  * rlen1).toInt & ~1, peakPixPos - 4 )

      // repaint only if pixel coords changed
      val peakPixChanged = peakPixPos != peakPixPosP
      val rmsPixChanged  = rmsPixPos  != rmsPixPosP
      val holdPixChanged = holdPixPos != holdPixPosP

      if( peakPixChanged || rmsPixChanged || holdPixChanged ) {
         var minPixPos = 0
         var maxPixPos = 0

         // calculate dirty span
         if( peakPixPos < peakPixPosP ) {
            minPixPos = peakPixPos
            maxPixPos = peakPixPosP
         } else {
            minPixPos = peakPixPosP
            maxPixPos = peakPixPos
         }
         if( holdPaintedVar ) {
            if( holdPixPos < holdPixPosP ) {
               if( holdPixPos < minPixPos )  minPixPos = holdPixPos
               if( holdPixPosP > maxPixPos ) maxPixPos = holdPixPosP
            } else {
               if( holdPixPosP < minPixPos ) minPixPos = holdPixPosP
               if( holdPixPos > maxPixPos )  maxPixPos = holdPixPos
            }
         }
         if( rmsPaintedVar ) {
            if( rmsPixPos < rmsPixPosP ) {
               if( rmsPixPos < minPixPos )  minPixPos = rmsPixPos
               if( rmsPixPosP > maxPixPos ) maxPixPos = rmsPixPosP
            } else {
               if( rmsPixPosP < minPixPos ) minPixPos = rmsPixPosP
               if( rmsPixPos > maxPixPos )  maxPixPos = rmsPixPos
            }
         }

         var c: Container = null
         var offX = 0
         var offY = 0

         if( refreshParent ) {
            c		= getParent
            offX	= ins.left + getX
            offY	= ins.top  + getY
         } else {
            c     = this
            offX  = ins.left
            offY	= ins.top
         }

         // trigger repaint
         if( vertical ) {
            c.repaint( offX, offY + rlen1 - maxPixPos, w1, maxPixPos - minPixPos + 2 )
         } else {
            c.repaint( offX + minPixPos, offY, maxPixPos - minPixPos + 2, h1 )
         }

      } else {
         peakToPaint		= -160f
         rmsToPaint		= -160f
         holdToPaint		= -160f
      }

      result
   }

   private def recalcPaint() {
      val imgLen		= (recentLength + 1) & ~1
      val imgBrdth	= recentBreadth
      var imgW       = 0
      var imgH       = 0

      if( imgPeak != null ) {
         imgPeak.flush()
         imgPeak = null
      }
      if( imgRMS != null ) {
         imgRMS.flush()
         imgRMS = null
      }

      if( vertical ) {	// ---- vertical ----
         if( (imgBg == null) || (imgBg.getWidth != imgBrdth) ) {
            if( imgBg != null ) {
               imgBg.flush()
               imgBg = null
            }
            val pix = if( imgBrdth == 10 ) {
               bgPixels
            } else {
               widenPixV( bgPixels, 10, imgBrdth, 2 );
            }
            imgBg = new BufferedImage( imgBrdth, 2, BufferedImage.TYPE_INT_ARGB )
            imgBg.setRGB( 0, 0, imgBrdth, 2, pix, 0, imgBrdth )
            pntBg = new TexturePaint( imgBg, new Rectangle( 0, 0, imgBrdth, 2 ))
         }
         imgW = imgBrdth
         imgH = imgLen

      } else {	// ---- horizontal ----
         if( (imgBg == null) || (imgBg.getHeight != imgBrdth) ) {
            if( imgBg != null ) {
               imgBg.flush()
               imgBg = null
            }
            val pix = widenPixH( bgPixels, 10, imgBrdth, 2 )
            imgBg = new BufferedImage( 2, imgBrdth, BufferedImage.TYPE_INT_ARGB )
            imgBg.setRGB( 0, 0, 2, imgBrdth, pix, 0, 2 )
            pntBg = new TexturePaint( imgBg, new Rectangle( 0, 0, 2, imgBrdth ))
         }
         imgW = imgLen
         imgH = imgBrdth
      }
      val pix1 = hsbFade( imgBrdth, imgLen, rmsTopColor, rmsBotColor, vertical )
      imgRMS = new BufferedImage( imgW, imgH, BufferedImage.TYPE_INT_ARGB )
      imgRMS.setRGB( 0, 0, imgW, imgH, pix1, 0, imgW )

      val pix2 = hsbFade( imgBrdth, imgLen, peakTopColor, peakBotColor, vertical )
      imgPeak = new BufferedImage( imgW, imgH, BufferedImage.TYPE_INT_ARGB )
      imgPeak.setRGB( 0, 0, imgW, imgH, pix2, 0, imgW )

      calcedLength	= recentLength
      calcedBreadth	= recentBreadth
   }

   override def paintComponent( g: Graphics ) {
      super.paintComponent( g )

      val w    = getWidth
      val h    = getHeight
      val w1   = w - (ins.left + ins.right)
      val h1   = h - (ins.top + ins.bottom)

      val len1 = if( vertical ) {
         recentBreadth	= w1
         h1
      } else {
         recentBreadth	= h1
         w1
      }
      val rlen1   = (len1 - 1) & ~1
      val len     = rlen1 + 1

      g.setColor( Color.black )
      g.fillRect( 0, 0, w, h )
      if( len <= 0 ) return

      if( len != recentLength ) {
         holdPixPos		= (holdNorm * rlen1).toInt & ~1
         peakPixPos		= (peakNorm * rlen1).toInt & ~1
         rmsPixPos		= math.min( (rmsNorm  * rlen1).toInt & ~1, peakPixPos - 4 )
         recentLength	= len
      }
      if( (calcedLength != recentLength) || (calcedBreadth != recentBreadth) ) {
         recalcPaint()
      }

      val g2      = g.asInstanceOf[ Graphics2D ]
      val atOrig  = g2.getTransform

      if( vertical ) {	// ---- vertical ----
         g2.translate( ins.left, ins.top + (len1 - len) )
         g2.setPaint( pntBg )
         val holdPixPosI = rlen1 - holdPixPos
         val peakPixPosI = rlen1 - peakPixPos
         if( rmsPaintedVar ) {
            val rmsPixPosI  = rlen1 - rmsPixPos
            g2.fillRect( 0, 0, recentBreadth, math.min( len, rmsPixPosI ))
            if( holdPaintedVar && (holdPixPos >= 0) ) {
               g2.drawImage( imgPeak, 0, holdPixPosI, recentBreadth, holdPixPosI + 1,
                                      0, holdPixPosI, recentBreadth, holdPixPosI + 1, this )
            }
            if( peakPixPos >= 0 ) {
               val lenClip = math.min( len, rmsPixPosI - 2 )
               g2.drawImage( imgPeak, 0, peakPixPosI, recentBreadth, lenClip,
                                      0, peakPixPosI, recentBreadth, lenClip, this )
            }
            if( rmsPixPos >= 0 ) {
               g2.drawImage( imgRMS, 0, rmsPixPosI, recentBreadth, len,
                                     0, rmsPixPosI, recentBreadth, len, this )
            }
         } else {
            g2.fillRect( 0, 0, recentBreadth, peakPixPosI )
            if( holdPaintedVar && (holdPixPos >= 0) ) {
               g2.drawImage( imgPeak, 0, holdPixPosI, recentBreadth, holdPixPosI + 1,
                                      0, holdPixPosI, recentBreadth, holdPixPosI + 1, this )
            }
            if( peakPixPos >= 0 ) {
               g2.drawImage( imgPeak, 0, peakPixPosI, recentBreadth, len,
                                      0, peakPixPosI, recentBreadth, len, this )
            }
         }
      } else {	// ---- horizontal ----
         g2.translate( ins.left, ins.top )
         g2.setPaint( pntBg )
         if( rmsPaintedVar ) {
            val rmsPixPosC = math.max( 0, rmsPixPos )
            g2.fillRect( rmsPixPosC, 0, len - rmsPixPosC, recentBreadth )
            if( holdPaintedVar && (holdPixPos >= 0) ) {
               g2.drawImage( imgPeak, holdPixPos, 0, holdPixPos + 1, recentBreadth,
                                      holdPixPos, 0, holdPixPos + 1, recentBreadth, this )
            }
            if( peakPixPos >= 0 ) {
               val offClip = math.max( 0, rmsPixPos + 3 )
               g2.drawImage( imgPeak, offClip, 0, peakPixPos + 1, recentBreadth,
                                      offClip, 0, peakPixPos + 1, recentBreadth, this )
            }
            if( rmsPixPos >= 0 ) {
               g2.drawImage( imgRMS, 0, 0, rmsPixPos + 1, recentBreadth,
                                     0, 0, rmsPixPos + 1, recentBreadth, this )
            }
         } else {
            val peakPixPosC = math.max( 0, peakPixPos )
            g2.fillRect( peakPixPosC, 0, len - peakPixPosC, recentBreadth )
            if( holdPaintedVar && (holdPixPos >= 0) ) {
               g2.drawImage( imgPeak, holdPixPos, 0, holdPixPos + 1, recentBreadth,
                                      holdPixPos, 0, holdPixPos + 1, recentBreadth, this )
            }
            if( peakPixPos >= 0 ) {
               g2.drawImage( imgPeak, 0, 0, peakPixPos + 1, recentBreadth,
                                      0, 0, peakPixPos + 1, recentBreadth, this )
            }
         }
      }

      peakToPaint	= -160f
      rmsToPaint	= -160f
      holdToPaint	= -160f
      peakPixPosP	= peakPixPos
      rmsPixPosP	= rmsPixPos
      holdPixPosP	= holdPixPos

      g2.setTransform( atOrig )
   }

   // --------------- Disposable interface ---------------

   private def disposeImages() {
      if( imgPeak != null ) {
         imgPeak.flush()
         imgPeak = null
      }
      if( imgRMS != null ) {
         imgRMS.flush()
         imgRMS = null
      }
      if( imgBg != null ) {
         imgBg.flush()
         imgBg	= null
         pntBg	= null
      }
      calcedLength = -1
   }

   def dispose() {
      disposeImages()
   }
}