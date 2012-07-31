/*
 *  Transport.java
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2012 Hanns Holger Rutz. All rights reserved.
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

import java.awt.{RenderingHints, Graphics, Component, Graphics2D, BasicStroke, Shape, Color, LinearGradientPaint, Paint}
import scala.Array
import java.awt.geom.{RoundRectangle2D, AffineTransform, Ellipse2D, Area, Rectangle2D, GeneralPath}
import javax.swing.{AbstractAction, Icon, JComponent, JButton, AbstractButton, BoxLayout, Box}
import collection.immutable.{IndexedSeq => IIdxSeq}
import java.awt.event.ActionEvent

object Transport {
   sealed trait ColorScheme {
      private[Transport] def shadowPaint: Paint
      private[Transport] def outlinePaint: Paint
      private[Transport] def fillPaint( scale: Float ): Paint
   }
   case object LightScheme extends ColorScheme {
      private[Transport] def fillPaint( scale: Float ): Paint = new LinearGradientPaint( 0f, 0f, 0f, scale * 19f, Array( 0f, 0.25f, 1f ),
         Array( new Color( 0xE8, 0xE8, 0xE8 ), new Color( 0xFF, 0xFF, 0xFF ), new Color( 0xF0, 0xF0, 0xF0 ))
      )
      private[Transport] val shadowPaint: Paint = new Color( 0, 0, 0, 0x50 )
      private[Transport] val outlinePaint: Paint = new Color( 0, 0, 0, 0xC0 )
   }
   case object DarkScheme extends ColorScheme {
      private[Transport] def fillPaint( scale: Float ): Paint = new LinearGradientPaint( 0f, 0f, 0f, scale * 19f, Array( 0f, 0.25f, 1f ),
         Array( new Color( 0x28, 0x28, 0x28 ), new Color( 0x00, 0x00, 0x00 ), new Color( 0x20, 0x20, 0x20 ))
      )
      private[Transport] val shadowPaint: Paint = new Color( 0xFF, 0xFF, 0xFF, 0x50 )
      private[Transport] val outlinePaint: Paint = Color.white
   }

   sealed trait Element {
      final def icon( scale: Float = 1f, colorScheme: ColorScheme = DarkScheme ) : Icon =
         new IconImpl( this, scale, colorScheme )

      final def apply( fun: => Unit ) : ActionElement = {
         new ActionElementImpl( this, fun )
      }
      def defaultXOffset: Float
      def defaultYOffset: Float
      def shape( scale: Float = 1f, xoff: Float = defaultXOffset, yoff: Float = defaultYOffset ) : Shape
   }

   private final class ActionElementImpl( val element: Element, fun: => Unit )
   extends ActionElement {
      def apply( scale: Float, colorScheme: ColorScheme ) : Action = {
         val icn = new IconImpl( element, scale, colorScheme )
         new Impl( icn, fun )
      }
   }

   case object Play extends Element {
      val defaultXOffset = 4f
      val defaultYOffset = 0f
      def shape( scale: Float, xoff: Float, yoff: Float ) : Shape = {
         val gp = new GeneralPath()
         gp.moveTo( xoff, yoff )
         gp.lineTo( xoff + scale * 15f, yoff + scale * 10f )
         gp.lineTo( xoff, yoff + scale * 20f )
         gp.closePath()
         gp
      }
   }
   case object Stop extends Element {
      val defaultXOffset = 3f
      val defaultYOffset = 2f
      def shape( scale: Float, xoff: Float, yoff: Float ) : Shape =
         new Rectangle2D.Float( xoff, yoff, scale * 16f, scale * 16f )
   }
   case object Pause extends Element {
      val defaultXOffset = 3f
      val defaultYOffset = 2f
      def shape( scale: Float, xoff: Float, yoff: Float ) : Shape = {
         val res = new Area( new Rectangle2D.Float(  xoff, yoff, scale * 6f, scale * 16f ))
         res.add( new Area(   new Rectangle2D.Float( xoff + scale * 10f, yoff, scale * 6f, scale * 16f )))
         res
      }
   }
   case object Record extends Element {
      val defaultXOffset = 3f
      val defaultYOffset = 2f
      def shape( scale: Float, xoff: Float, yoff: Float ) : Shape =
         new Ellipse2D.Float( scale * xoff, scale * yoff, scale * 16f, scale * 16f )
   }
   case object GoToBegin extends Element {
      val defaultXOffset = 4f
      val defaultYOffset = 3f
      def shape( scale: Float, xoff: Float, yoff: Float ) : Shape = {
         val end = GoToEnd.shape( scale, xoff = 0f, yoff = yoff )
         val at = AffineTransform.getScaleInstance( -1.0, 1.0 )
         at.translate( -(end.getBounds2D.getWidth + xoff), 0 )
         at.createTransformedShape( end )
      }
   }
   case object GoToEnd extends Element {
      val defaultXOffset = 4f
      val defaultYOffset = 3f
      def shape( scale: Float, xoff: Float, yoff: Float ) : Shape = {
         val play = Play.shape( scale * 0.7f, xoff = xoff, yoff = yoff )
         val res  = new Area( play )
         val ba   = new Rectangle2D.Float( scale * 11.5f + xoff, yoff, scale * 3f, scale * 14f )
         res.add( new Area( ba ))
         res
      }
   }
   case object FastForward extends Element {
      val defaultXOffset = 0f
      val defaultYOffset = 3f
      def shape( scale: Float, xoff: Float, yoff: Float ) : Shape = {
         val play = Play.shape( scale * 0.7f, xoff = xoff, yoff = yoff )
         val p2   = AffineTransform.getTranslateInstance( scale * 11.5f, 0 ).createTransformedShape( play )
         val res  = new Area( play )
         res.add( new Area( p2 ))
         res
      }
   }
   case object Rewind extends Element {
      val defaultXOffset = 0f
      val defaultYOffset = 3f
      def shape( scale: Float, xoff: Float, yoff: Float ) : Shape = {
         val ffwd = FastForward.shape( scale, xoff = 0, yoff = yoff )
         val at = AffineTransform.getScaleInstance( -1.0, 1.0 )
         at.translate( -ffwd.getBounds2D.getWidth - xoff, 0 )
         at.createTransformedShape( ffwd )
      }
   }
   case object Loop extends Element {
      private val doRotate = true

      val defaultXOffset = 0f
      val defaultYOffset = if( doRotate ) 3f else 1.5f
      def shape( scale: Float = 1f, xoff: Float = 0f, yoff: Float = 3f ) : Shape = {

         val res = new Area( new RoundRectangle2D.Float( 0f, scale * 4f, scale * 22f, scale * 14f, scale * 10f, scale * 10f ))
         res.subtract( new Area( new RoundRectangle2D.Float( 0f + scale * 3f, scale * 7f, scale * 16f, scale * 8f, scale * 8f, scale * 8f )))

         val gp = new GeneralPath()
         gp.moveTo( 0f, scale * 18f )
         gp.lineTo( scale * 11f, scale * 9f )
         gp.lineTo( scale * 11f, 0f )
         gp.lineTo( scale * 22f, 0f )
         gp.lineTo( scale * 22f, scale * 18f )

         gp.closePath()
         res.subtract( new Area( gp ))
         val play = Play.shape( scale * 0.5f, /* xoff + */ 9f, /* yoff - 2.5f */ 0.5f )
         res.add( new Area( play ))
         val rot = AffineTransform.getRotateInstance( math.Pi, scale * 11f, scale * 12f ).createTransformedShape( res )
         res.add( new Area( rot ))
         val at = AffineTransform.getScaleInstance( 1f, 0.8f )
         if( doRotate ) {
            at.rotate( math.Pi * -0.2, scale * 11f, scale * 12f )
            at.preConcatenate( AffineTransform.getTranslateInstance( xoff, yoff - 3f ))
         } else {
            at.translate( xoff, yoff )
         }
         at.createTransformedShape( res )
      }
   }

   private val segmentFirst   = "first"
   private val segmentMiddle  = "middle"
   private val segmentLast    = "last"
   private val segmentOnly    = "only"

   private final class ButtonStripImpl( actions: Seq[ Action ], scheme: ColorScheme )
   extends Box( BoxLayout.X_AXIS ) with ButtonStrip {
      private val (buttonSeq, buttonMap, elementMap) = {
         var bMap = Map.empty[ Element, AbstractButton ]
         var tMap = Map.empty[ AbstractButton, Action ]
         var sq   = IIdxSeq.empty[ AbstractButton ]
         val it = actions.iterator
         if( it.hasNext ) {
            val n1   = it.next()
            val pos1 = if( it.hasNext ) segmentFirst else segmentOnly
            val b1   = makeButton( pos1, n1 )
            bMap += n1.element -> b1
            tMap += b1 -> n1
            sq :+= b1
            while( it.hasNext ) {
               val n    = it.next()
               val pos  = if( it.hasNext ) segmentMiddle else segmentLast
               val b    = makeButton( pos, n )
               bMap += n.element -> b
               tMap += b -> n
               sq :+= b
            }
         }
         (sq, bMap, tMap)
      }

      buttonSeq.foreach( add )

      def buttons: Seq[ AbstractButton ] = buttonSeq
      def button( element: Element ) : Option[ AbstractButton ] = buttonMap.get( element )
      def elements: Seq[ Element ] = actions.map( _.element )
      def element( button: AbstractButton ) : Option[ Element ] = elementMap.get( button ).map( _.element )

      private def makeButton( pos: String = "only", action: Action ) : JButton = {
         val b = new JButton( action )
         b.setFocusable( false )
         b.putClientProperty( "JButton.buttonType", "segmentedCapsule" )   // "segmented" "segmentedRoundRect" "segmentedCapsule" "segmentedTextured" "segmentedGradient"
         b.putClientProperty( "JButton.segmentPosition", pos )
//         b.setMinimumSize( new Dimension( 10, 50 ))
//         b.setPreferredSize( new Dimension( 50, 50 ))
         b
      }

      override def toString = "ButtonStrip"
   }

   def makeButtonStrip( actions: Seq[ ActionElement ], scheme: ColorScheme = DarkScheme ) : JComponent with ButtonStrip = {
      val a = actions.map( _.apply( 0.8f, scheme ))
      new ButtonStripImpl( a, scheme )
   }

   private val strk        = new BasicStroke( 1f )
   private val shadowYOff  = 1f

   trait ButtonStrip {
      def buttons: Seq[ AbstractButton ]
      def button( iconType: Element ) : Option[ AbstractButton ]
      def elements: Seq[ Element ]
      def element( button: AbstractButton ) : Option[ Element ]
   }

//      val pntFill1    = new LinearGradientPaint( 0f, 0f, 0f, 19f, Array( 0f, 0.45f, 0.55f, 1f ),
//         Array( new Color( 0xF0, 0xF0, 0xF0 ), new Color( 0xFF, 0xFF, 0xFF ), new Color( 0xE8, 0xE8, 0xE8 ),
//                new Color( 0xFF, 0xFF, 0xFF ))
//      )
//
//      val colrFill   = new Color( 0xFF, 0xFF, 0xFF )

//      val pntFill2    = new LinearGradientPaint( 0f, 0f, 0f, 19f, Array( 0f, 0.25f, 1f ),
//         Array( new Color( 0xFF, 0xFF, 0xFF ), new Color( 0xF0, 0xF0, 0xF0 ), new Color( 0xFF, 0xFF, 0xFF ))
//      )


//      def loopShape1 : Shape = {
//         val res = new Area( new RoundRectangle2D.Float( 0f, 4f, 26f, 14f, 10f, 10f ))
//         res.subtract( new Area( new RoundRectangle2D.Float( 3f, 7f, 20f, 8f, 8f, 8f )))
//         res.subtract( new Area( new Rectangle2D.Float( 9f, 0f, 10f, 10f )))
//         val play = playShape( 0.5f, xoff = 9f, yoff = 0.5f )
//         res.add( new Area( play ))
//         res
//      }

   def paint( g: Graphics2D, iconType: Element, x: Float = 0f, y: Float = 0f, scale: Float = 1f, colorScheme: ColorScheme = DarkScheme ) {
      val x1         = iconType.defaultXOffset * scale
      val y1         = iconType.defaultYOffset * scale
//         val x1 = x
//         val y1 = y
      val inShape    = iconType.shape( scale, x1, y1 )
      val outShape   = calcOutShape( inShape )
      paintImpl( g, inShape, outShape, x - x1, y - y1, scale, colorScheme )
   }

   private def calcOutShape( inShape: Shape ) : Shape = {
      val out = new Area( strk.createStrokedShape( inShape ))
      out.add( new Area( inShape ))
      out
   }

   private def paintImpl( g2: Graphics2D, inShape: Shape, outShape: Shape, x: Float, y: Float, scale: Float, scheme: ColorScheme ) {
//            g2.setColor( Color.red )
//            g2.fillRect( x.toInt, y.toInt, math.ceil(24 * scale).toInt, math.ceil(22 * scale).toInt )
         val atOrig  = g2.getTransform
         g2.translate( x + 1, y + 1 + shadowYOff )
         g2.setPaint( scheme.shadowPaint )
         g2.fill( outShape )
         g2.translate( 0, -shadowYOff )
         g2.setPaint( scheme.outlinePaint )
         g2.fill( outShape )
         g2.setPaint( scheme.fillPaint( scale ))
         g2.fill( inShape )
         g2.setTransform( atOrig )
   }

   private final class IconImpl( val element: Element, val scale: Float, scheme: ColorScheme )
   extends Icon {
      private val inShape: Shape = element.shape( scale )
      private val outShape: Shape = calcOutShape( inShape )

      def getIconWidth  = math.ceil( 24 * scale ).toInt
      def getIconHeight = math.ceil( 22 * scale ).toInt

      def paintIcon( c: Component, g: Graphics, x: Int, y: Int ) {
         val g2 = g.asInstanceOf[ Graphics2D ]
         g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON )
         g2.setRenderingHint( RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE )
         paintImpl( g2, inShape, outShape, x, y, scale, scheme )
      }
   }

   private final class Impl( icn: IconImpl, fun: => Unit ) extends AbstractAction( null, icn ) with Action {
      def icon: Icon = icn
      def element: Element = icn.element
      def scale: Float = icn.scale

      def actionPerformed( e: ActionEvent) {
         fun
      }
   }

   sealed trait ActionElement {
      def element: Element
      def apply( scale: Float = 1f, colorScheme: ColorScheme = DarkScheme ) : Action
   }

   sealed trait Action extends javax.swing.Action {
      def icon: Icon
      def element: Element
      def scale: Float
   }
}
