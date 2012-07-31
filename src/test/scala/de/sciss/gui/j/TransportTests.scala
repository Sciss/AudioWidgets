package de.sciss.gui.j

import java.awt.{Paint, BorderLayout, Dimension, Component, LinearGradientPaint, RenderingHints, Color, BasicStroke, Shape, Graphics2D, Graphics, EventQueue}
import javax.swing.{JComponent, JButton, Icon, Box, WindowConstants, JPanel, JFrame}
import java.awt.geom.{RoundRectangle2D, Ellipse2D, AffineTransform, Rectangle2D, Area, GeneralPath}

object TransportTests extends App with Runnable {
   EventQueue.invokeLater( this )

   object TransportIcon {
      sealed trait ColorScheme {
         private[TransportIcon] def shadowPaint: Paint
         private[TransportIcon] def outlinePaint: Paint
         private[TransportIcon] def fillPaint: Paint
      }
      case object LightScheme extends ColorScheme {
         private[TransportIcon] val fillPaint: Paint = new LinearGradientPaint( 0f, 0f, 0f, 19f, Array( 0f, 0.25f, 1f ),
            Array( new Color( 0xE8, 0xE8, 0xE8 ), new Color( 0xFF, 0xFF, 0xFF ), new Color( 0xF0, 0xF0, 0xF0 ))
         )
         private[TransportIcon] val shadowPaint: Paint = new Color( 0, 0, 0, 0x50 )
         private[TransportIcon] val outlinePaint: Paint = new Color( 0, 0, 0, 0xC0 )
      }
      case object DarkScheme extends ColorScheme {
         private[TransportIcon] val fillPaint: Paint = new LinearGradientPaint( 0f, 0f, 0f, 19f, Array( 0f, 0.25f, 1f ),
            Array( new Color( 0x28, 0x28, 0x28 ), new Color( 0x00, 0x00, 0x00 ), new Color( 0x20, 0x20, 0x20 ))
         )
         private[TransportIcon] val shadowPaint: Paint = new Color( 0xFF, 0xFF, 0xFF, 0x50 )
         private[TransportIcon] val outlinePaint: Paint = Color.white
      }

      sealed trait Type {
         final def apply( scale: Float = 1f, colorScheme: ColorScheme = DarkScheme ) : TransportIcon =
            new Impl( this, scale, colorScheme )
         def defaultXOffset: Float
         def defaultYOffset: Float
         def shape( scale: Float = 1f, xoff: Float = defaultXOffset, yoff: Float = defaultYOffset ) : Shape
      }

      case object Play extends Type {
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
      case object Stop extends Type {
         val defaultXOffset = 3f
         val defaultYOffset = 2f
         def shape( scale: Float, xoff: Float, yoff: Float ) : Shape =
            new Rectangle2D.Float( xoff, yoff, scale * 16f, scale * 16f )
      }
      case object Pause extends Type {
         val defaultXOffset = 3f
         val defaultYOffset = 2f
         def shape( scale: Float, xoff: Float, yoff: Float ) : Shape = {
            val res = new Area( new Rectangle2D.Float(  xoff, yoff, scale * 6f, scale * 16f ))
            res.add( new Area(   new Rectangle2D.Float( xoff + scale * 10f, yoff, scale * 6f, scale * 16f )))
            res
         }
      }
      case object Record extends Type {
         val defaultXOffset = 3f
         val defaultYOffset = 2f
         def shape( scale: Float, xoff: Float, yoff: Float ) : Shape =
            new Ellipse2D.Float( scale * xoff, scale * yoff, scale * 16f, scale * 16f )
      }
      case object GoToBegin extends Type {
         val defaultXOffset = 4f
         val defaultYOffset = 3f
         def shape( scale: Float, xoff: Float, yoff: Float ) : Shape = {
            val end = GoToEnd.shape( scale, xoff = 0f, yoff = yoff )
            val at = AffineTransform.getScaleInstance( -1.0, 1.0 )
            at.translate( -(end.getBounds2D.getWidth + xoff), 0 )
            at.createTransformedShape( end )
         }
      }
      case object GoToEnd extends Type {
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
      case object FastForward extends Type {
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
      case object Rewind extends Type {
         val defaultXOffset = 0f
         val defaultYOffset = 3f
         def shape( scale: Float, xoff: Float, yoff: Float ) : Shape = {
            val ffwd = FastForward.shape( scale, xoff = xoff, yoff = yoff )
            val at = AffineTransform.getScaleInstance( -1.0, 1.0 )
            at.translate( -ffwd.getBounds2D.getWidth, 0 )
            at.createTransformedShape( ffwd )
         }
      }
      case object Loop extends Type {
         private val doRotate = true

         val defaultXOffset = 0f
         val defaultYOffset = if( doRotate ) 3f else 1.5f
         def shape( scale: Float = 1f, xoff: Float = 0f, yoff: Float = 3f ) : Shape = {

            val res = new Area( new RoundRectangle2D.Float( xoff, scale * 4f, scale * 22f, scale * 14f, scale * 10f, scale * 10f ))
            res.subtract( new Area( new RoundRectangle2D.Float( xoff + scale * 3f, scale * 7f, scale * 16f, scale * 8f, scale * 8f, scale * 8f )))

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

      private val strk        = new BasicStroke( 1f )
      private val shadowYOff  = 1f

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

      def paint( g: Graphics2D, iconType: Type, x: Float = 0f, y: Float = 0f, scale: Float = 1f, colorScheme: ColorScheme = DarkScheme ) {
         val inShape    = iconType.shape( scale, x, y )
         val outShape   = calcOutShape( inShape )
         paintImpl( g, inShape, outShape, 0f, 0f, colorScheme )
      }

      private def calcOutShape( inShape: Shape ) : Shape = {
         val out = new Area( strk.createStrokedShape( inShape ))
         out.add( new Area( inShape ))
         out
      }

      private def paintImpl( g2: Graphics2D, inShape: Shape, outShape: Shape, x: Float, y: Float, scheme: ColorScheme ) {
   //         g2.setColor( Color.red )
   //         g2.fillRect( x, y, 24, 22 )
            val atOrig  = g2.getTransform
            g2.translate( x + 1, y + 1 + shadowYOff )
            g2.setPaint( scheme.shadowPaint )
            g2.fill( outShape )
            g2.translate( 0, -shadowYOff )
            g2.setPaint( scheme.outlinePaint )
            g2.fill( outShape )
            g2.setPaint( scheme.fillPaint )
            g2.fill( inShape )
            g2.setTransform( atOrig )
      }

      private final class Impl( val iconType: Type, val scale: Float, scheme: ColorScheme ) extends TransportIcon {
         private val inShape: Shape = iconType.shape( scale )
         private val outShape: Shape = calcOutShape( inShape )

         def getIconWidth  = math.ceil( 24 * scale ).toInt
         def getIconHeight = math.ceil( 22 * scale ).toInt

         def paintIcon( c: Component, g: Graphics, x: Int, y: Int ) {
            val g2 = g.asInstanceOf[ Graphics2D ]
            g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON )
            g2.setRenderingHint( RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE )
            paintImpl( g2, inShape, outShape, x, y, scheme )
         }
      }
   }
   sealed trait TransportIcon extends Icon {
      def iconType: TransportIcon.Type
      def scale: Float
   }

   def run() {
//      def paintShape( g2: Graphics2D, x: Int, y: Int, shape: Shape ) {
//         g2.setColor( Color.red )
//         g2.fillRect( x, y, 24, 22 )
//         val strk    = new BasicStroke( 1f )
//         val out     = new Area( strk.createStrokedShape( shape ))
//         out.add( new Area( shape ))
//         val atOrig  = g2.getTransform
//         g2.translate( x + 1, y + 1 + shadowYOff )
//         g2.setColor( colrShadow )
//         g2.fill( out )
//         g2.translate( 0, -shadowYOff )
//         g2.setColor( colrBorder )
//         g2.fill( out )
//         g2.setPaint( pntFill )
//         g2.fill( shape )
//         g2.setTransform( atOrig )
//      }

      val f = new JFrame()
      val p = new JComponent {
         setPreferredSize( new Dimension( 260, 70 ))
         override def paintComponent( g: Graphics ) {
            import TransportIcon.{paint => pnt, _}
            val g2 = g.asInstanceOf[ Graphics2D ]
            g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON )

            def gaga( scheme: ColorScheme, yoff: Int ) {
               Seq( GoToBegin, Rewind, Play, Stop, Pause, FastForward, GoToEnd, Record, Loop ).zipWithIndex.foreach {
                  case (icn, idx) =>
                     pnt( g2, icn, 20 + icn.defaultXOffset + (idx * 28), yoff + icn.defaultYOffset, 1f, scheme )
               }
            }
            gaga( DarkScheme, 10 )
            gaga( LightScheme, 40 )
         }
      }

      def but( pos: String = "only", shape: TransportIcon.Type, scheme: TransportIcon.ColorScheme = TransportIcon.DarkScheme ) : JButton = {
         val b = new JButton( shape( 0.8f, scheme ))
         b.setFocusable( false )
         b.putClientProperty( "JButton.buttonType", "segmentedCapsule" )   // "segmented" "segmentedRoundRect" "segmentedCapsule" "segmentedTextured" "segmentedGradient"
         b.putClientProperty( "JButton.segmentPosition", pos )
//         b.setMinimumSize( new Dimension( 10, 50 ))
//         b.setPreferredSize( new Dimension( 50, 50 ))
         b
      }

      val sq = IndexedSeq( TransportIcon.GoToBegin, TransportIcon.Rewind, TransportIcon.Play, TransportIcon.Stop,
         TransportIcon.FastForward, TransportIcon.GoToEnd, TransportIcon.Loop )
      val sqSz = sq.size

      def mkButtons( scheme: TransportIcon.ColorScheme = TransportIcon.DarkScheme ) : JComponent = {
         val p = Box.createHorizontalBox()
         sq.zipWithIndex.foreach {
            case (icn, idx) =>
               val pos = if( sqSz == 1 ) "only" else if( idx == 0 ) "first" else if( idx == sqSz - 1 ) "last" else "middle"
               p.add( but( pos, icn, scheme ))
         }
         p
      }

      val butP = new JPanel( new BorderLayout() )
      butP.add( mkButtons(), BorderLayout.NORTH )
      butP.add( mkButtons( TransportIcon.LightScheme ), BorderLayout.SOUTH )

      val cp = f.getContentPane
      cp.add( p, BorderLayout.NORTH )
      cp.add( butP, BorderLayout.SOUTH )

      f.pack() // f.setSize( 300, 200 )
      f.setResizable( false )
      f.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE )
      f.setVisible( true )
   }
}
