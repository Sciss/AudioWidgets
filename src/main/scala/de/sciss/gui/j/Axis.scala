/*
 *  Axis.scala
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

import java.awt.geom.{ GeneralPath, AffineTransform }
import java.awt.image.{ BufferedImage }
import java.text.{ MessageFormat }
import java.util.{ Locale }
import annotation.switch
import java.awt.{Font, Paint, Color, Dimension, FontMetrics, Graphics, Graphics2D, Rectangle, RenderingHints, TexturePaint}
import javax.swing.{UIManager, SwingConstants, JComponent}

trait AxisLike {
   def fixedBounds : Boolean
   def fixedBounds_= (b: Boolean): Unit
//   def format : Format
//   def format_= (f: Format): Unit
   def inverted : Boolean
   def inverted_= (b: Boolean): Unit
   def maximum : Double
   def maximum_= (value: Double): Unit
   def minimum : Double
   def minimum_= (value: Double): Unit
}

trait AxisCompanion {
   sealed trait Format
   object Format {
      case object Decimal extends Format
      case object Integer extends Format
      final case class Time( hours: Boolean = false, millis: Boolean = true ) extends Format
   }
}

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
object Axis extends AxisCompanion {
 	private val DECIMAL_RASTER	   = Array( 100000000L, 10000000L, 1000000L, 100000L, 10000L, 1000L, 100L, 10L, 1L )
	private val INTEGERS_RASTER	= Array( 100000000L, 10000000L, 1000000L, 100000L, 10000L, 1000L )
	private val TIME_RASTER		   = Array( 60000000L, 6000000L, 600000L, 60000L, 10000L, 1000L, 100L, 10L, 1L )
	private val MIN_LABSPC		   = 16

	// the following are used for Number to String conversion using MessageFormat
	private val msgNormalPtrn = Array(
      "{0,number,0}",
		"{0,number,0.0}",
		"{0,number,0.00}",
		"{0,number,0.000}"
   )
   private val msgTimePtrn	= Array(
      "{0,number,integer}:{1,number,00}",
		"{0,number,integer}:{1,number,00.0}",
		"{0,number,integer}:{1,number,00.00}",
		"{0,number,integer}:{1,number,00.000}"
   )
   private val msgTimeHoursPtrn	= Array(
      "{0,number,integer}:{1,number,00}:{2,number,00}",
		"{0,number,integer}:{1,number,00}:{2,number,00.0}",
		"{0,number,integer}:{1,number,00}:{2,number,00.00}",
		"{0,number,integer}:{1,number,00}:{2,number,00.000}"
   )

	private val pntBarGradientPixels = Array( 0xFFB8B8B8, 0xFFC0C0C0, 0xFFC8C8C8, 0xFFD3D3D3,
									  0xFFDBDBDB, 0xFFE4E4E4, 0xFFEBEBEB, 0xFFF1F1F1,
									  0xFFF6F6F6, 0xFFFAFAFA, 0xFFFBFBFB, 0xFFFCFCFC,
									  0xFFF9F9F9, 0xFFF4F4F4, 0xFFEFEFEF )
	private val barExtent		= pntBarGradientPixels.length

   private class Label( val name: String, val pos: Int )

//   sealed trait Format
//   object Format {
//      case object Decimal extends Format
//      case object Integer extends Format
//      final case class Time( hours: Boolean = false, millis: Boolean = true ) extends Format
//   }
}

class Axis( orient: Int = SwingConstants.HORIZONTAL )
extends JComponent with AxisLike {
   import Axis._
   import SwingConstants.{HORIZONTAL, VERTICAL}

   private var orientVar      = orient

   private var recentWidth    = 0
   private var recentHeight   = 0
   private var doRecalc		   = true

   private val kPeriod        = 1000.0
   private var labels         = new Array[ Label ]( 0 )
   private val shpTicks       = new GeneralPath()

   private val msgForm        = new MessageFormat( msgNormalPtrn( 0 ), Locale.US )
   private val msgArgs        = new Array[ AnyRef ]( 3 )

	private val trnsVertical   = new AffineTransform()

   private var msgPtrn: Array[ String ] = null
   private var labelRaster: Array[ Long ] = null
   private var labelMinRaster = 0L

   private var spcMin   = 0.0
   private var spcMax   = 0.0 // 1.0

   private var formatVar : Format = Format.Decimal
   private var flMirroir      = false
   private var flTimeFormat   = false
   private var flTimeHours    = false
   private var flTimeMillis   = false
   private var flIntegers     = false
   private var flFixedBounds  = false

   private var imgWidth       = 0
   private var imgHeight      = 0
   private var img : BufferedImage = null
   private var pntBackground : TexturePaint = null

   private def orientUpdated() {
      (orientVar: @switch) match {
         case HORIZONTAL =>
            setMaximumSize( new Dimension( getMaximumSize.width, barExtent ))
            setMinimumSize( new Dimension( getMinimumSize.width, barExtent ))
            setPreferredSize( new Dimension( getPreferredSize.width, barExtent ))
            imgWidth    = 1
            imgHeight   = barExtent
         case VERTICAL =>
            setMaximumSize( new Dimension( barExtent, getMaximumSize.height ))
            setMinimumSize( new Dimension( barExtent, getMinimumSize.height ))
            setPreferredSize( new Dimension( barExtent, getPreferredSize.height ))
            imgWidth    = barExtent
            imgHeight   = 1
      }
      if( img != null ) img.flush()
      img = new BufferedImage( imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB )
      img.setRGB( 0, 0, imgWidth, imgHeight, pntBarGradientPixels, 0, imgWidth )
      pntBackground = new TexturePaint( img, new Rectangle( 0, 0, imgWidth, imgHeight ))
      triggerRedisplay()
   }

//     private var viewPortVar: Option[ JViewport ] = None

    // ---- constructor ----
   orientUpdated()
   flagsUpdated()

   setFont({
      val f = UIManager.getFont( "Slider.font", Locale.US )
      if( f != null ) f.deriveFont( math.min( f.getSize2D, 9.5f )) else new Font( "SansSerif", Font.PLAIN, 9 )
   })
   setOpaque( true )

//   def viewport : Option[ JViewport ] = viewPortVar
//   def viewport_=( v: JViewport ) { viewPortVar = v }

//   def flags = flagsVar
//	def flags_=( newFlags: Int ) {
//		if( flagsVar == newFlags ) return
//        flagsVar = newFlags
//        flagsUpdated()
//    }

   def orientation : Int = orientVar
   def orientation_=( orient: Int ) {
      if( orientVar != orient ) {
         if( orient != HORIZONTAL && orient != VERTICAL ) throw new IllegalArgumentException( orient.toString )
         orientVar = orient
         orientUpdated()
      }
   }

	/**
	 *	Flag: Defines the axis to have flipped min/max values.
	 *	I.e. for horizontal orient, the maximum value
	 *	corresponds to the left edge, for vertical orient
	 *	the maximum corresponds to the bottom edge
	 */
   def inverted : Boolean = flMirroir
   def inverted_=( b: Boolean ) {
      if( flMirroir != b ) {
         flMirroir = b
         flagsUpdated()
      }
   }

	/*
	 *	Flag: Requests that the space's min and max are always displayed
	 *		  and hence subdivision are made according to the bounds
	 */
   def fixedBounds : Boolean = flFixedBounds
   def fixedBounds_=( b: Boolean ) {
      if( flFixedBounds != b ) {
         flFixedBounds = b
         flagsUpdated()
      }
   }

   def format : Format = formatVar
   def format_=( f: Format ) {
      if( formatVar != f ) {
         formatVar      = f
         f match {
            case Format.Integer =>
               flIntegers     = true
               flTimeFormat   = false
            case Format.Time( hours, millis ) =>
               flIntegers     = false
               flTimeFormat   = true
               flTimeHours    = hours
               flTimeMillis   = millis
            case Format.Decimal =>
               flIntegers     = false
               flTimeFormat   = false
         }
         flagsUpdated()
      }
   }

//   /**
//    *	Flag: Requests the labels to be formatted as MIN:SEC.MILLIS
//    */
//   def timeFormat : Boolean = flTimeFormat
//   def timeFormat_=( b: Boolean ) {
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
//   def intFormat_=( b: Boolean ) {
//      if( flIntegers != b ) {
//         flIntegers = b
//         if( b ) flTimeFormat = false
//         flagsUpdated()
//      }
//   }

   private def flagsUpdated() {
//	   flMirroir		= (flags & MIRROIR) != 0
//		flTimeFormat	= (flags & TIMEFORMAT) != 0
//		flIntegers		= (flags & INTEGERS) != 0
//		flFixedBounds	= (flags & FIXEDBOUNDS) != 0

		if( flTimeFormat ) {
			msgPtrn		= if( flTimeHours ) msgTimeHoursPtrn else msgTimePtrn
			labelRaster	= TIME_RASTER
		} else {
			msgPtrn		= msgNormalPtrn
			labelRaster	= if( flIntegers ) INTEGERS_RASTER else DECIMAL_RASTER
		}
		labelMinRaster	= labelRaster( labelRaster.length - 1 )

		triggerRedisplay()
	}

   def minimum : Double = spcMin
   def minimum_=( value: Double ) {
      if( value != spcMin ) {
         spcMin = value
         triggerRedisplay()
      }
	}

   def maximum : Double = spcMax
   def maximum_=( value: Double ) {
      if( value != spcMax ) {
         spcMax = value
         triggerRedisplay()
      }
	}

//   protected def setSpaceNoRepaint( newSpace: VectorSpace ) {
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
//   protected def viewRectChanged( r: Rectangle ) {}

	override def paintComponent( g: Graphics ) {
		super.paintComponent( g )

		val g2        = g.asInstanceOf[ Graphics2D ]
		val trnsOrig  = g2.getTransform
		val fm        = g2.getFontMetrics

      val r = /* if( viewPortVar.isEmpty ) */ normalBounds /* else portBounds */

		if( doRecalc || (r.width != recentWidth) || (r.height != recentHeight) ) {
			recentWidth		= r.width
			recentHeight	= r.height
			recalcLabels( g )
			if( orientVar == VERTICAL ) recalcTransforms()
			doRecalc		= false
		}

		g2.setPaint( pntBackground )
		g2.fillRect( r.x, r.y, r.width, r.height )

      g2.translate( r.x, r.y )
      val aaOrig = g2.getRenderingHint( RenderingHints.KEY_ANTIALIASING )
		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF )

		val y = if( orient == VERTICAL ) {
			g2.transform( trnsVertical )
			r.width - 2 /* 3 */ - fm.getMaxDescent
		} else {
			r.height - 2 /* 3 */ - fm.getMaxDescent
		}
		g2.setColor( Color.lightGray )
		g2.draw( shpTicks )

		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON )
		g2.setColor( Color.black )

      labels.foreach( l => {
			g2.drawString( l.name, l.pos, y )
		})

		g2.setTransform( trnsOrig )
//      paintOnTop( g2 )
      g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, aaOrig )
   }

	private def recalcTransforms() {
		trnsVertical.setToRotation( -math.Pi / 2, recentHeight.toDouble / 2,
										     recentHeight.toDouble / 2 )
	}

	private def calcStringWidth( fntMetr: FontMetrics, value: Double ) : Int = {
		if( flTimeFormat ) {
         val secs    = value % 60
         val mins0   = (value / 60).toInt
         if( flTimeHours ) {
            val hours   = mins0 / 60
            val mins    = mins0 % 60
            msgArgs( 0 )   = hours.asInstanceOf[ AnyRef ]
            msgArgs( 1 )   = mins.asInstanceOf[ AnyRef ]
            msgArgs( 2 )   = secs.asInstanceOf[ AnyRef ]
         } else {
            msgArgs( 0 )   = mins0.asInstanceOf[ AnyRef ]
            msgArgs( 1 )   = secs.asInstanceOf[ AnyRef ]
         }
		} else {
			msgArgs( 0 )	= value.asInstanceOf[ AnyRef ]
		}
		fntMetr.stringWidth( msgForm.format( msgArgs ))
	}

	private def calcMinLabSpc( fntMetr: FontMetrics, mini: Double, maxi: Double ) : Int = {
		math.max( calcStringWidth( fntMetr, mini ), calcStringWidth( fntMetr, maxi )) + MIN_LABSPC
	}

   private def recalcLabels( g: Graphics ) {
      import math._

      val fntMetr	= g.getFontMetrics

      shpTicks.reset()
      if( spcMin == spcMax ) {
         labels    = new Array[ Label ]( 0 )
         return
      }

      val (width, height) = if( orientVar == HORIZONTAL ) {
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
      val scale	= width / (spcMax - spcMin)
      val minK	= kPeriod * spcMin
      val maxK	= kPeriod * spcMax

      val isInteger = flIntegers || (flTimeFormat && !flTimeMillis)
      var (numTicks: Int, valueOff: Double, pixelOff: Double, valueStep: Double) =
         if( flFixedBounds ) {
            val ptrnIdx1 = if( isInteger ) 0 else {
               val ptrnIdxTmp = {
                  val n = abs( minK ).toLong
                  if( (n % 1000) == 0 ) {
                     0
                  } else if( (n % 100) == 0 ) {
                     1
                  } else if( (n % 10) == 0 ) {
                     2
                  } else {
                     3
                  }
               }

               val n = abs( maxK ).toLong
               if( (n % 1000) == 0 ) {
                  ptrnIdxTmp
               } else if( (n % 100) == 0 ) {
                  max( ptrnIdxTmp, 1 )
               } else if( (n % 10) == 0 ) {
                  max( ptrnIdxTmp, 2 )
               } else {
                  3
               }
            }

            // make a first label width calculation with coarsest display
            msgForm.applyPattern( msgPtrn( ptrnIdx1 ))
            val minLbDist	= calcMinLabSpc( fntMetr, spcMin, spcMax )
            var numLabels	= max( 1, width / minLbDist )

            // ok, easy way : only divisions by powers of two
            var shift = 0
            while( numLabels > 2 ) {
               shift += 1
               numLabels >>= 1
            }
            numLabels <<= shift
            val valueStep	= (maxK - minK) / numLabels

            val ptrnIdx2 = if( isInteger ) 0 else {
               val n = valueStep.toLong
               if( (n % 1000) == 0 ) {
                  ptrnIdx1
               } else if( (n % 100) == 0 ) {
                  max( ptrnIdx1, 1 )
               } else if( (n % 10) == 0 ) {
                  max( ptrnIdx1, 2 )
               } else {
                  3
               }
            }

            if( ptrnIdx2 != ptrnIdx1 ) {	// ok, labels get bigger, recalc numLabels ...
               msgForm.applyPattern( msgPtrn( ptrnIdx2 ))
               val minLbDist = calcMinLabSpc( fntMetr, spcMin, spcMax )
               numLabels = max( 1, width / minLbDist )
               shift = 0
               while( numLabels > 2 ) {
                  shift += 1
                  numLabels >>= 1
               }
               numLabels <<= shift
               val valueStep = (maxK - minK) / numLabels

               // nochmal ptrnIdx berechnen, evtl. reduziert sich die aufloesung wieder...
               msgForm.applyPattern( msgPtrn({
                  val n = valueStep.toLong
                  if( (n % 1000) == 0 ) {
                     ptrnIdx1
                  } else if( (n % 100) == 0 ) {
                     max( ptrnIdx1, 1 )
                  } else if( (n % 10) == 0 ) {
                     max( ptrnIdx1, 2 )
                  } else {
                     3
                  }
               }))
			   }

            (4, minK, 0, valueStep)

         } else { // ---- no fixed bounds ----

            // make a first label width calculation with coarsest display
            msgForm.applyPattern( msgPtrn( 0 ))
            var minLbDist = calcMinLabSpc( fntMetr, spcMin, spcMax )
            var numLabels = max( 1, width / minLbDist )

            // now valueStep =^= 1000 * minStep
            var valueStep = ceil( (maxK - minK) / numLabels )
            // die Grossenordnung von valueStep ist Indikator fuer Message Pattern
            var ptrnIdx1 = if( isInteger ) 0 else 3
            var raster = labelMinRaster
            var i = 0; var break = false
            while( (i < labelRaster.length) && !break ) {
               if( valueStep >= labelRaster( i )) {
                  ptrnIdx1  = max( 0, i - 5 )
                  raster    = labelRaster( i )
                  break     = true
               } else {
                  i += 1
               }
            }
            msgForm.applyPattern( msgPtrn( ptrnIdx1 ))
            if( ptrnIdx1 > 0 ) {	// have to recheck label width!
               minLbDist	= max( calcStringWidth( fntMetr, spcMin ), calcStringWidth( fntMetr, spcMax )) + MIN_LABSPC
               numLabels	= max( 1, width / minLbDist )
               valueStep	= ceil( (maxK - minK) / numLabels )
            }
            valueStep	= max( 1, floor( (valueStep + raster - 1) / raster ))
            if( valueStep == 7 || valueStep == 9 ) valueStep = 10

            val numTicks = (valueStep.toInt: @switch) match {
               case 2 => 4
               case 4 => 4
               case 8 => 4
               case 3 => 6
               case 6 => 6
               case _ => 5
            }
            valueStep   *= raster
            val valueOff = floor( abs( minK ) / valueStep ) * (if( minK >= 0 ) valueStep else -valueStep)
            val pixelOff = (valueOff - minK) / kPeriod * scale + 0.5

            (numTicks, valueOff, pixelOff, valueStep)
         }

      val pixelStep   = valueStep / kPeriod * scale
      var tickStep	= pixelStep / numTicks
      val numLabels	= max( 0, ((width - pixelOff + pixelStep - 1.0) / pixelStep).toInt )

      if( labels.length != numLabels ) labels = new Array[ Label ]( numLabels )

      if( flMirroir ) {
         pixelOff	= width - pixelOff
         tickStep	= -tickStep
      }

      var i = 0
      while( i < numLabels ) {
         if( flTimeFormat ) {
            val mins0   = (valueOff / 60000).toInt
            val secs    = (valueOff % 60000) / 1000
            if( flTimeHours ) {
               val hours   = mins0 / 60
               val mins    = mins0 % 60
               msgArgs( 0 )	= hours.asInstanceOf[ AnyRef ]
               msgArgs( 1 )   = mins.asInstanceOf[ AnyRef ]
               msgArgs( 1 )	= secs.asInstanceOf[ AnyRef ]
            } else {
               msgArgs( 0 )	= mins0.asInstanceOf[ AnyRef ]
               msgArgs( 1 )	= secs.asInstanceOf[ AnyRef ]
            }
			} else {
				msgArgs( 0 )	= (valueOff / kPeriod).asInstanceOf[ AnyRef ]
			}
         labels( i ) = new Label( msgForm.format( msgArgs ), (pixelOff + 2).toInt )
			valueOff += valueStep
			shpTicks.moveTo( pixelOff.toFloat, 1 )
			shpTicks.lineTo( pixelOff.toFloat, height - 2 )
			pixelOff += tickStep
            var k = 1
            while( k < numTicks ) {
				shpTicks.moveTo( pixelOff.toFloat, height - 4 )
				shpTicks.lineTo( pixelOff.toFloat, height - 2 )
				pixelOff += tickStep
            k += 1
			}
         i += 1
		}
	}

	private def triggerRedisplay() {
		doRecalc = true
//		if( host.isDefined ) {
//			host.get.update( this )
//		} else
      if( isVisible ) {
			repaint()
		}
	}

	// -------------- Disposable interface --------------

	def dispose() {
		labels      = null
		shpTicks.reset()
		img.flush()
	}
}