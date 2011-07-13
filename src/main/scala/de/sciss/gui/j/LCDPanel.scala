/*
 *  LCDPanel.java
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

import javax.swing.JPanel
import java.awt.{Graphics2D, LinearGradientPaint, Insets, Color, Graphics}
import java.beans.{PropertyChangeEvent, PropertyChangeListener}

/**
 * Unfinished!
 *
 * TODO: It seems for other colors to work nicely, we should offset saturation and brightness,
 * instead of having the factors.
 */
class LCDPanel extends JPanel {
   private var hue = 0.194f
   private var sat = 0.190f
   private var bri = 0.805f

   private val gradInnerColrs    = new Array[ Color ]( 5 )
   private val gradInnerFrac     = Array( 0f, -1f, 0.5f, -1f, 1f )
   private var colrTop : Color   = null
   private var colrTopSh : Color = null
   private val colrBot           = new Color( 0xFF, 0xFF, 0xFF, 0x7F )
   private var colrBotSh : Color = null
   private var recentHeight      = -1
   private val in                = new Insets( 0, 0, 0, 0 )
   private var gradInner : LinearGradientPaint = null

   setBackground( Color.getHSBColor( hue, sat, bri ))
   recalcColors()

   addPropertyChangeListener( "background", new PropertyChangeListener {
      def propertyChange( e: PropertyChangeEvent ) {
         val c = getBackground
         val arr = Color.RGBtoHSB( c.getRed, c.getGreen, c.getBlue, null )
         hue = arr( 0 )
         sat = arr( 1 )
         bri = arr( 2 )
         recalcColors()
         repaint()
      }
   })

   override def getInsets : Insets = getInsets( new Insets( 0, 0, 0, 0 ))
   override def getInsets( insets: Insets ) : Insets = {
      super.getInsets( insets )
      insets.top    += 3
      insets.left   += 3
      insets.bottom += 3
      insets.right  += 3
      insets
   }

   private def mixColor( hueOffset: Float, satFactor: Float, briFactor: Float ) =
      Color.getHSBColor( hue + hueOffset,
                         math.max( 0f, math.min( 1f, sat * satFactor )),
                         math.max( 0f, math.min( 1f, bri * briFactor )))

   private def recalcColors() {
      gradInnerColrs( 0 )  = mixColor( 0.006f, 0.737f, 1.019f )
      gradInnerColrs( 1 )  = mixColor( 0f,     0.737f, 1.043f )
      gradInnerColrs( 2 )  = mixColor( 0f,     0.895f, 1.019f )
      gradInnerColrs( 3 )  = mixColor( 0f,     1.105f, 0.981f )
      gradInnerColrs( 4 )  = mixColor( 0f,     0.684f, 1.056f )
      colrTop              = mixColor( 0.006f, 0.632f, 0.398f )
      colrTopSh            = mixColor( 0f,     1.737f, 0.795f )
      colrBotSh            = mixColor( 0f,     0.684f, 0.932f )
   }

   private def recalcGradients( h: Int ) {
      val hi = math.max( 1, h - 4 )
      val f1 = math.min( 0.499f, 1f / hi )
      val f3 = math.min( 0.999f, 0.5f + 1f / hi )
      gradInnerFrac( 1 ) = f1
      gradInnerFrac( 3 ) = f3
      gradInner = new LinearGradientPaint( 2f, 0f, 0f, (2 + hi).toFloat, gradInnerFrac, gradInnerColrs )
      recentHeight = h
   }

   override def paintComponent( g: Graphics ) {
      getInsets( in )
      val h = getHeight - (in.top + in.bottom)
      val w = getWidth - (in.left + in.right)
      if( h != recentHeight ) recalcGradients( h )
      val g2 = g.asInstanceOf[ Graphics2D ]
      val atOrig = g2.getTransform
      g2.translate( in.left, in.top )
      g2.setColor( colrTop )
      g2.drawLine( 0, 0, w - 1, 0 )
      g2.setColor( colrTopSh )
      g2.drawLine( 0, 1, w - 1, 1 )
      g2.setPaint( gradInner )
      g2.fillRect( 0, 2, w - 1, h - 4 )
      g2.setColor( colrBotSh )
      g2.drawLine( 0, h - 2, w - 1, h - 2 )
      g2.setColor( colrBot )
      g2.drawLine( 0, h - 1, w - 1, h - 1 )
      g2.setTransform( atOrig )
   }
}