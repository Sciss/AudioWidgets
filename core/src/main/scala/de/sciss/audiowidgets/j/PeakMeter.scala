/*
 *  PeakMeter.java
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2013 Hanns Holger Rutz. All rights reserved.
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

package de.sciss.audiowidgets
package j

import annotation.switch
import javax.swing.{JPanel, BoxLayout, SwingConstants, BorderFactory}
import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.awt.{Insets, Font, Color, Graphics}
import collection.immutable.{IndexedSeq => IIdxSeq}

object PeakMeter {
   /* final */ val DefaultHoldDuration = 2500   // TODO: make final in binary incompatible next version
}
class PeakMeter( orient: Int = SwingConstants.VERTICAL ) extends JPanel with PeakMeterLike /* with PeakMeterView */ {
   import PeakMeter._
   import SwingConstants._

   private final var holdDurationVar	      = DefaultHoldDuration   // milliseconds peak hold
	protected var meters			         = new Array[ PeakMeterBar ]( 0 )
	protected var caption: PeakMeterCaption   = null
	private final var captionPositionVar	         = LEFT
	private final var captionAlign	               = RIGHT
	private final var captionVisibleVar	            = true
	private final var captionLabelsVar	            = true
	private final var numChannelsVar		            = 0
	private final var borderVisibleVar			      = false

	private final var rmsPaintedVar		            = true
	private final var holdPaintedVar		            = true

	private final var orientVar			            = VERTICAL
	private final var vertical		                  = true

   private final val ins                           = new Insets( 0, 0, 0, 0 )
   private final var ticksVar		                  = 101

   setLayout( new BoxLayout( this, BoxLayout.X_AXIS ))

   setFont( new Font( "SansSerif", Font.PLAIN, 12 ))
   addPropertyChangeListener( "font", new PropertyChangeListener {
      def propertyChange( e: PropertyChangeEvent ) {
         if( caption != null ) {
            caption.setFont( getFont )
            val b = BorderFactory.createEmptyBorder( caption.ascent, 1, caption.descent, 1 )
            var ch = 0; while( ch < meters.length ) {
               meters( ch ).setBorder( b )
            ch += 1 }
         }
      }
   })

	def orientation_=( value: Int ) {
		if( orientVar != value ) {
			if( value != HORIZONTAL && value != VERTICAL ) throw new IllegalArgumentException( value.toString )
			orientVar   = value
			vertical	= orientVar == VERTICAL
			if( caption != null ) caption.orientation = orientVar
			var ch = 0; while( ch < meters.length ) {
				meters( ch ).orientation = orientVar
			ch += 1 }
			setLayout( new BoxLayout( this, if( vertical ) BoxLayout.X_AXIS else BoxLayout.Y_AXIS ))
			updateBorders()
			revalidate()
		}
	}
   def orientation : Int = orientVar

   def channel( ch: Int ) : PeakMeterChannel = meters( ch )

   def ticks_=( num: Int ) {
      if( ticksVar != num ) {
         ticksVar = num
         if( caption != null ) {
            caption.ticks = num
         }
         var ch = 0; while( ch < meters.length ) {
            meters( ch ).ticks = num
         ch += 1 }
      }
   }
   def ticks : Int = ticksVar

   def rmsPainted_=( b: Boolean ) {
		if( rmsPaintedVar != b ) {
         rmsPaintedVar = b
         var ch = 0; while( ch < meters.length ) {
            meters( ch ).rmsPainted = b
         ch += 1 }
      }
	}
   def rmsPainted : Boolean = rmsPaintedVar

	def holdPainted_=( b: Boolean ) {
		if( holdPaintedVar != b ) {
         holdPaintedVar = b
         var ch = 0; while( ch < meters.length ) {
            meters( ch ).holdPainted = b
         ch += 1 }
      }
	}
   def holdPainted : Boolean = holdPaintedVar

//   def meterUpdate( peakRMSPairs: Array[ Float ], offset: Int, time: Long ) : Boolean = {
//      val metersCopy	= meters // = easy synchronization
//      val numMeters	= math.min( metersCopy.length, (peakRMSPairs.length - offset) >> 1 )
//      var dirty		= 0
//
//      var ch = 0; var off = offset; while( ch < numMeters ) {
//         val peak = peakRMSPairs( off ); off += 1
//         val rms  = peakRMSPairs( off ); off += 1
//         if( metersCopy( ch ).update( peak, rms, time )) dirty += 1
//      ch += 1 }
//
//      dirty > 0
//   }

	def update( values: IIdxSeq[ Float ], offset: Int, time: Long ) : Boolean = {
		val metersCopy	= meters // = easy synchronization
		val numMeters	= math.min( metersCopy.length, (values.length - offset) >> 1 )
		var dirty		= 0

		var ch = 0; var off = offset; while( ch < numMeters ) {
         val peak = values( off ); off += 1
         val rms  = values( off ); off += 1
			if( metersCopy( ch ).update( peak, rms, time )) dirty += 1
		ch += 1 }

		dirty > 0
	}

   def holdDuration_=( millis: Int ) {
      holdDurationVar = millis
      var ch = 0; while( ch < meters.length ) {
         meters( ch ).holdDuration = millis
      ch += 1 }
   }
   def holdDuration : Int = holdDurationVar

   def clearMeter() {
      var ch = 0; while( ch < meters.length ) {
         meters( ch ).clearMeter()
      ch += 1 }
   }

	def clearHold() {
		var ch = 0; while( ch < meters.length ) {
         meters( ch ).clearHold()
      ch += 1 }
	}

	def dispose() {
		var ch = 0; while( ch < meters.length ) {
         meters( ch ).dispose()
      ch += 1 }
	}

	// --------------- public methods ---------------

   def borderVisible_=( b: Boolean ) {
		if( borderVisibleVar != b ) {
         borderVisibleVar = b
         setBorder( if( b ) new RecessedBorder() else null )
         updateBorders()
      }
	}
   def borderVisible : Boolean = borderVisibleVar

	def hasCaption_=( b: Boolean ) {
		if( b == (caption != null) ) return

		if( b ) {
			caption = new PeakMeterCaption( orientVar )
			caption.setFont( getFont )
			caption.setVisible( captionVisibleVar )
			caption.horizontalAlignment   = captionAlign
			caption.labelsVisible         = captionLabelsVar
		} else {
			caption = null
		}
		rebuildMeters()
	}
   def hasCaption : Boolean = caption != null

	def captionPosition_=( pos: Int ) {
		if( captionPositionVar == pos ) return

		captionAlign = (pos: @switch) match {
         case LEFT   => RIGHT
         case RIGHT  => LEFT
         case CENTER => CENTER
         case _      => throw new IllegalArgumentException( pos.toString )
      }

      captionPositionVar = pos

		if( caption != null ) {
			caption.horizontalAlignment = captionAlign
			rebuildMeters()
		}
	}
   def captionPosition : Int = captionPositionVar

	def captionLabels_=( b: Boolean ) {
		if( captionLabelsVar != b ) {
         captionLabelsVar = b
         if( caption != null ) {
            caption.labelsVisible = captionLabelsVar
         }
      }
	}
   def captionLabels : Boolean = captionLabelsVar

	def captionVisible_=( b: Boolean ) {
		if( captionVisibleVar != b ) {
         captionVisibleVar = b
         if( caption != null ) {
            caption.setVisible( captionVisibleVar )
            updateBorders()
         }
      }
	}
   def captionVisible : Boolean = captionVisibleVar

   def numChannels_=( num: Int ) {
		if( numChannelsVar != num ) {
			numChannelsVar = num
			rebuildMeters()
		}
	}
	def numChannels : Int = numChannelsVar

	override def paintComponent( g: Graphics ) {
		super.paintComponent( g )

		getInsets( ins )
		g.setColor( Color.black )
		g.fillRect( ins.left, ins.top,
		            getWidth - (ins.left + ins.right),
		            getHeight - (ins.top + ins.bottom) )
	}

	// -------------- private methods --------------

	private def rebuildMeters() {
		removeAll()

		val b1		= if( caption == null ) null else BorderFactory.createEmptyBorder( caption.ascent, 1, caption.descent, 1 )
		val b2		= if( caption == null ) BorderFactory.createEmptyBorder( 1, 1, if( vertical ) 1 else 0, if( vertical ) 0 else 1 ) else
                                          BorderFactory.createEmptyBorder( caption.ascent, 1, caption.descent, 0 )

		val schnuck1 = if( !borderVisibleVar || (captionVisibleVar && captionPositionVar == RIGHT) ) numChannelsVar - 1 else -1
		val schnuck2 = if( captionVisibleVar && captionPositionVar == CENTER ) numChannelsVar >> 1 else -1

		val newMeters  = new Array[ PeakMeterBar ]( numChannels )
      val numChans   = numChannelsVar
		var ch = 0; while( ch < numChans ) {
         val m             = new PeakMeterBar( orientVar )
			m.refreshParent   = true
			m.rmsPainted      = rmsPaintedVar
			m.holdPainted     = holdPaintedVar
			if( (ch == schnuck1) || (ch == schnuck2) ) {
				if( b1 != null ) m.setBorder( b1 )
			} else {
				m.setBorder( b2 )
			}
			m.ticks           = ticksVar // if( caption != null ) ticksVar else 0
			add( m )
         newMeters( ch )   = m
		ch += 1 }
		if( caption != null ) {
			captionPositionVar match {
			   case LEFT   => add( caption, 0 )
			   case RIGHT  => add( caption )
			   case CENTER => add( caption, getComponentCount >> 1 )
			}
		}
		meters = newMeters
		revalidate()
		repaint()
	}

	private def updateBorders() {
		val b1		= if( caption == null ) BorderFactory.createEmptyBorder( 1, 1, 1, 1 ) else
                                          BorderFactory.createEmptyBorder( caption.ascent, 1, caption.descent, 1 )
		val b2		= if( caption == null ) BorderFactory.createEmptyBorder( 1, 1, if( vertical ) 1 else 0, if( vertical ) 0 else 1 ) else
                                          BorderFactory.createEmptyBorder( caption.ascent, 1, caption.descent, 0 )

		val schnuck1 = if( !borderVisibleVar || (captionVisibleVar && captionPositionVar == RIGHT )) numChannelsVar - 1 else -1
		val schnuck2 = if( captionVisibleVar && captionPositionVar == CENTER ) numChannelsVar >> 1 else -1

		var ch = 0; while( ch < numChannelsVar ) {
			if( (ch == schnuck1) || (ch == schnuck2) ) {
				meters( ch ).setBorder( b1 )
			} else {
				meters( ch ).setBorder( b2 )
			}
//			meters( ch ).ticks = 101
		ch += 1 }
	}
}