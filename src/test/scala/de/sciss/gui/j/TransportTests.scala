package de.sciss.gui.j

import java.awt.{BorderLayout, Dimension, Component, LinearGradientPaint, RenderingHints, Color, BasicStroke, Shape, Graphics2D, Graphics, EventQueue}
import javax.swing.{JComponent, JButton, Icon, Box, WindowConstants, JPanel, JFrame}
import java.awt.geom.{RoundRectangle2D, Ellipse2D, AffineTransform, Rectangle2D, Area, GeneralPath}

object TransportTests extends App with Runnable {
   EventQueue.invokeLater( this )

   object TransportIcon {
      case object Play      extends TransportIcon {

      }
      case object GoToBegin extends TransportIcon {

      }
      case object GoToEnd   extends TransportIcon {

      }
   }
   sealed trait TransportIcon

   def run() {
      val colrShadow = new Color( 0xFF, 0xFF, 0xFF, 0x50 ) // new Color( 0, 0, 0, 0x50 )
      val colrBorder = Color.white // new Color( 0, 0, 0, 0xC0 )
//      val colrFill   = new Color( 0xFF, 0xFF, 0xFF )
      val shadowYOff = 1f

      val pntFill1    = new LinearGradientPaint( 0f, 0f, 0f, 19f, Array( 0f, 0.45f, 0.55f, 1f ),
         Array( new Color( 0xF0, 0xF0, 0xF0 ), new Color( 0xFF, 0xFF, 0xFF ), new Color( 0xE8, 0xE8, 0xE8 ),
                new Color( 0xFF, 0xFF, 0xFF ))
      )

      val pntFill3   = new LinearGradientPaint( 0f, 0f, 0f, 19f, Array( 0f, 0.25f, 1f ),
         Array( new Color( 0xE8, 0xE8, 0xE8 ), new Color( 0xFF, 0xFF, 0xFF ), new Color( 0xF0, 0xF0, 0xF0 ))
      )

      val pntFill    = new LinearGradientPaint( 0f, 0f, 0f, 19f, Array( 0f, 0.25f, 1f ),
         Array( new Color( 0x28, 0x28, 0x28 ), new Color( 0x00, 0x00, 0x00 ), new Color( 0x20, 0x20, 0x20 ))
      )

      val pntFill2    = new LinearGradientPaint( 0f, 0f, 0f, 19f, Array( 0f, 0.25f, 1f ),
         Array( new Color( 0xFF, 0xFF, 0xFF ), new Color( 0xF0, 0xF0, 0xF0 ), new Color( 0xFF, 0xFF, 0xFF ))
      )

      def paintShape( g2: Graphics2D, x: Int, y: Int, shape: Shape ) {
         g2.setColor( Color.red )
         g2.fillRect( x, y, 24, 22 )
         val strk    = new BasicStroke( 1f )
         val out     = new Area( strk.createStrokedShape( shape ))
         out.add( new Area( shape ))
         val atOrig  = g2.getTransform
         g2.translate( x + 1, y + 1 + shadowYOff )
         g2.setColor( colrShadow )
         g2.fill( out )
         g2.translate( 0, -shadowYOff )
         g2.setColor( colrBorder )
         g2.fill( out )
         g2.setPaint( pntFill )
         g2.fill( shape )
         g2.setTransform( atOrig )
      }

      def playShape( scale: Float = 1f, xoff: Float = 4f, yoff: Float = 0f ) : Shape = {
         val gp = new GeneralPath()
         val sx = scale * xoff
         val sy = scale * yoff
         gp.moveTo( sx, sy )
         gp.lineTo( sx + scale * 15f, sy + scale * 10f )
         gp.lineTo( sx, sy + scale * 20f )
         gp.closePath()
         gp
      }

      def ffwdShape( scale: Float = 1f, xoff: Float = 0f, yoff: Float = 3f ) : Shape = {
         val play = playShape( scale * 0.7f, xoff = xoff / 0.7f, yoff = yoff / 0.7f )
         val p2   = AffineTransform.getTranslateInstance( scale * 11.5f, 0 ).createTransformedShape( play )
         val res  = new Area( play )
         res.add( new Area( p2 ))
         res
      }

      def beginShape( scale: Float = 1f, xoff: Float = 4f, yoff: Float = 3f ) : Shape = {
         val end = endShape( scale, xoff = 0f, yoff = yoff )
         val at = AffineTransform.getScaleInstance( -1.0, 1.0 )
         at.translate( -(end.getBounds2D.getWidth + scale * xoff), 0 )
         at.createTransformedShape( end )
      }

      def endShape( scale: Float = 1f, xoff: Float = 4f, yoff: Float = 3f ) : Shape = {
         val play = playShape( scale * 0.7f, xoff = xoff, yoff = /* scale * */ yoff / 0.7f )
         val res  = new Area( play )
         val ba   = new Rectangle2D.Float( scale * (11.5f + xoff), scale * yoff, scale * 3f, scale * 14f )
         res.add( new Area( ba ))
         res
      }

      def rwdShape( scale: Float = 1f, xoff: Float = 0f, yoff: Float = 3f ) : Shape = {
         val ffwd = ffwdShape( scale, xoff = xoff, yoff = yoff )
         val at = AffineTransform.getScaleInstance( -1.0, 1.0 )
         at.translate( -ffwd.getBounds2D.getWidth, 0 )
         at.createTransformedShape( ffwd )
      }

      def stopShape( scale: Float = 1f, xoff: Float = 3f, yoff: Float = 2f ) : Shape =
         new Rectangle2D.Float( scale * xoff, scale * yoff, scale * 16f, scale * 16f )

      def pauseShape( sz: Float = 20f ) : Shape = {
         val res = new Area( new Rectangle2D.Float(  0f, sz * 0.1f, sz * 0.3f, sz * 0.8f ))
         res.add( new Area(   new Rectangle2D.Float( sz * 0.5f, sz * 0.1f, sz * 0.3f, sz * 0.8f )))
         res
      }

      def recShape( scale: Float = 1f, xoff: Float = 3f, yoff: Float = 2f ) : Shape =
         new Ellipse2D.Float( scale * xoff, scale * yoff, scale * 16f, scale * 16f )

      def loopShape1 : Shape = {
         val res = new Area( new RoundRectangle2D.Float( 0f, 4f, 26f, 14f, 10f, 10f ))
         res.subtract( new Area( new RoundRectangle2D.Float( 3f, 7f, 20f, 8f, 8f, 8f )))
         res.subtract( new Area( new Rectangle2D.Float( 9f, 0f, 10f, 10f )))
         val play = playShape( 0.5f, xoff = 9f, yoff = 0.5f )
         res.add( new Area( play ))
         res
      }

      def loopShape( scale: Float = 1f ) : Shape = {
         val sz = 20f * scale
         val res = new Area( new RoundRectangle2D.Float( 0f, sz * 0.2f, sz * 1.1f, sz * 0.7f, sz * 0.5f, sz * 0.5f ))
         res.subtract( new Area( new RoundRectangle2D.Float( sz * 0.15f, sz * 0.35f, sz * 0.8f, sz * 0.4f, sz * 0.4f, sz * 0.4f )))
         val gp = new GeneralPath()
         gp.moveTo( 0f, sz * 0.9f )
         gp.lineTo( sz * 0.55f, sz * 0.45f )
         gp.lineTo( sz * 0.55f, 0f )
         gp.lineTo( sz * 1.1f, 0f )
         gp.lineTo( sz * 1.1f, sz * 0.9f )
         gp.closePath()
         res.subtract( new Area( gp ))
         val play = playShape( scale * 0.5f, /* scale * */ 9f, /* scale * */ 0.5f )
//         val play = {
//            val gp = new GeneralPath()
//            gp.moveTo( 12f, 4f )
//            gp.lineTo( 17f, 4f )
//            gp.lineTo( 12f, 11f )
//            gp.closePath()
//            gp
//         }
         res.add( new Area( play ))
         val rot = AffineTransform.getRotateInstance( math.Pi, sz * 0.55f, sz * 0.6f ).createTransformedShape( res )
         res.add( new Area( rot ))
         val at = AffineTransform.getScaleInstance( 1f, 0.8f )
         at.rotate( math.Pi * -0.2, sz * 0.55f, sz * 0.6f )
//         at.rotate( math.Pi * -0.1, 11f, 12f )
         at.createTransformedShape( res )
      }

      val f = new JFrame()
      val p = new JComponent {
         setPreferredSize( new Dimension( 260, 50 ))
         override def paintComponent( g: Graphics ) {
            val g2 = g.asInstanceOf[ Graphics2D ]
            g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON )
            paintShape( g2, 20, 10, beginShape() )
            paintShape( g2, 45, 10, rwdShape() )
            paintShape( g2, 80, 10, playShape() )
            paintShape( g2, 105, 10, stopShape() )
            paintShape( g2, 130, 10, pauseShape() )
            paintShape( g2, 155, 10, ffwdShape() )
            paintShape( g2, 185, 10, endShape() )
            paintShape( g2, 210, 10, recShape() )
            paintShape( g2, 235, 10, loopShape() )
         }
      }
      val p1 = Box.createHorizontalBox()

      def mkIcon( shape: Shape, yOff: Int = 1 ): Icon = new Icon {
         def paintIcon( c: Component, g: Graphics, x: Int, y: Int) {
            val g2 = g.asInstanceOf[ Graphics2D ]
            g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON )
            paintShape( g2, x + 1, y + yOff, shape )
         }

         def getIconWidth = 16
         def getIconHeight = 16
      }

      def but( pos: String = "only", shape: Shape, yOff: Int = 1 ) {
         val b = new JButton( mkIcon( shape, yOff ))
         b.setFocusable( false )
         b.putClientProperty( "JButton.buttonType", "segmentedCapsule" )   // "segmented" "segmentedRoundRect" "segmentedCapsule" "segmentedTextured" "segmentedGradient"
         b.putClientProperty( "JButton.segmentPosition", pos )
//         b.setMinimumSize( new Dimension( 10, 50 ))
//         b.setPreferredSize( new Dimension( 50, 50 ))
         p1.add( b )
      }

      but( "first", beginShape( 18 ), -1 )
      but( "middle", playShape( 14 ))
      but( "last",   stopShape( 0.7f ))

      val cp = f.getContentPane
      cp.add( p, BorderLayout.NORTH )
      cp.add( p1, BorderLayout.SOUTH )

      f.setSize( 300, 200 )
      f.setResizable( false )
      f.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE )
      f.setVisible( true )
   }
}
