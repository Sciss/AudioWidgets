package de.sciss.audiowidgets.j

import java.awt.{Point, GridLayout, FlowLayout, Color, BorderLayout, RenderingHints, Graphics2D, Graphics, Dimension, EventQueue}
import javax.swing.{JComboBox, JPanel, WindowConstants, JFrame, JComponent}
import java.awt.event.{MouseEvent, MouseAdapter, ActionListener, ActionEvent}
import WavePainter.MultiResolution

object WaveTests extends App with Runnable {
   EventQueue.invokeLater( this )

//   private def ??? = sys.error( "TODO" )

   def run() {
      val dataLen    = 128 // 64
      val freq       = 2 * math.Pi / (dataLen - 1)
      val dataFull   = Array.tabulate( dataLen ) { i => math.sin( i * freq ).toFloat }

      def sincDecim( over: Int ) = {
         val factor  = 256 // 32 // 16
         val dl2     = dataLen * over
         val fullLen = dl2 * factor
         val halfLen = fullLen / 2
         var a0      = math.random
         val full    = Array.tabulate( fullLen ) { i =>
            val j = i - halfLen
            val k = j * freq * 4/(factor * over)
            val s = math.sin( k ) / k
            val r = math.random
            a0    = a0 * 0.96 + r * 0.04
            (s * a0).toFloat
         }
         val res = new Array[ Float ]( dl2 * 3 )
         WavePainter.Decimator.pcmToPeakRMS( factor ).decimate( full, 0, res, 0, dl2 )
         res
      }

      val dataDecim  = sincDecim( 1 )
      val dataDecim2 = {
         val factor  = 256
         val first   = sincDecim( factor )
//         val fl      = first.length
         val res     = new Array[ Float ]( first.length / factor )
         WavePainter.Decimator.peakRMS( factor ).decimate( first, 0, res, 0, res.length / 3 )
         res
      }

      val pntSH      = WavePainter.sampleAndHold
      val pntLin     = WavePainter.linear
      val pntPeakRMS = WavePainter.peakRMS

      val pntAll  = pntLin :: pntSH :: pntPeakRMS :: pntPeakRMS :: Nil

//      pnt.zoomX.sourceLow  = 0

      pntAll.foreach { pnt =>
         pnt.scaleX.sourceHigh = dataLen - 1
         pnt.scaleY.sourceLow  = -1
         pnt.scaleY.sourceHigh = 1
   //      pnt.zoomX.targetLow  = 0
         pnt.scaleY.targetHigh = 0
         pnt match {
            case one: WavePainter.OneLayer =>
               one.color = Color.white
            case multi: WavePainter.PeakRMS =>
               multi.peakColor   = Color.gray
               multi.rmsColor    = Color.white
         }
      }

      var pnt: WavePainter       = pntLin
      var data: Array[ Float ]   = dataFull

      abstract class SimpleView extends JComponent {
         setPreferredSize( new Dimension( 260, 180 ))
         setFocusable( true )
         addMouseListener( new MouseAdapter {
            override def mousePressed( e: MouseEvent ) { if( isEnabled ) requestFocus() }
         })

         override def paintComponent( g: Graphics ) {
            val g2 = g.asInstanceOf[ Graphics2D ]
            g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON )
            g2.setRenderingHint( RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE  )
            g2.setColor( Color.black )
            val w = getWidth
            val h = getHeight
            g2.fillRect( 0, 0, w, h )
            paint( g2, w, h )
         }

         protected def paint( g: Graphics2D, width: Int, height: Int ) : Unit
      }

      val view1 = new SimpleView {
         def paint( g: Graphics2D, w: Int, h: Int ) {
            pnt.scaleX.targetHigh = w
            pnt.scaleY.targetLow  = h - 1
            pnt.paint( g, data, 0, dataLen )
         }
      }

      val panel = new JPanel( new FlowLayout() )
      val ggPnt   = new JComboBox( Array[ AnyRef ]( "Linear", "Sample+Hold", "Peak+RMS 1", "Peak+RMS 2" ))
      panel.add( ggPnt )
      ggPnt.addActionListener( new ActionListener {
         def actionPerformed( e: ActionEvent ) {
            val idx = ggPnt.getSelectedIndex
            pnt      = pntAll( idx )
            data     = if( pnt == pntPeakRMS ) {
               if( idx == 2 ) dataDecim else dataDecim2
            } else {
               dataFull
            }
            view1.repaint()
         }
      })

      val multiSize  = 120000 // 131072
      val fullData   = {
         val r       = new util.Random( 0L )
         def iter()  = r.nextDouble() * 2 - 1
         var a       = iter()
         val freq    = 8 * 2 * math.Pi / (multiSize - 1)
         Array.tabulate( multiSize ) { j =>
            val i = iter()
            a = a * 0.96 + i * 0.04
            (a * math.cos( j * freq )).toFloat
         }
      }
      val mSrc = MultiResolution.Source.wrap( Array( fullData ))

      lazy val multi = WavePainter.MultiResolution( mSrc, display )
      multi.peakColor   = Color.gray
//      multi.peakColor   = new java.awt.LinearGradientPaint( 0f, 0f, 0f, 3200f, Array( 0f, 0.25f, 0.75f, 1f ), Array( Color.red, Color.yellow, Color.yellow, Color.red ))
      multi.rmsColor    = Color.white
      multi.stopFrame   = multiSize

      implicit def richDouble( x: Double ) = new RichDouble( x )
      final class RichDouble( x: Double ) {
         def linlin( srcLo: Double, srcHi: Double, dstLo: Double, dstHi: Double ) =
            (x - srcLo) / (srcHi - srcLo) * (dstHi - dstLo) + dstLo

         def linexp( srcLo: Double, srcHi: Double, dstLo: Double, dstHi: Double) =
            math.pow( dstHi / dstLo, (x- srcLo) / (srcHi - srcLo) ) * dstLo
      }

      lazy val view2 = new SimpleView {
         def paint( g: Graphics2D, w: Int, h: Int ) { multi.paint( g )}
      }
      lazy val display: WavePainter.Display = new WavePainter.Display {
         def numChannels = 1
         def numFrames = multiSize
//         def refreshChannel( ch: Int ) { view2.repaint() }
         def refreshAllChannels() { view2.repaint() }

         def channelDimension( result: Dimension ) { view2.getSize( result )}
         def channelLocation( ch: Int, result: Point ) { result.x = 0; result.y = 0 }
      }

      WavePainter.HasZoom.defaultKeyActions( multi, display ).foreach( _.install( view2 ))
      view2.addMouseWheelListener( WavePainter.HasZoom.defaultMouseWheelAction( multi, display ))

      val f    = new JFrame()
      val cp   = f.getContentPane

      val p1 = new JPanel( new BorderLayout() )
      p1.add( view1, BorderLayout.CENTER )
      p1.add( panel, BorderLayout.NORTH  )

      val p2 = new JPanel( new BorderLayout() )
      p2.add( view2, BorderLayout.CENTER )
//      val p4 = Box.createHorizontalBox()
//      val p5 = Box.createVerticalBox()
//      p5.add( ggStart )
//      p5.add( ggStop )
//      p4.add( p5 )
//      p4.add( Box.createHorizontalStrut( ggZoomY.getPreferredSize.width ))
//      p2.add( p4, BorderLayout.SOUTH )
//      p2.add( ggZoomY, BorderLayout.EAST )

      val p3 = new JPanel( new GridLayout( 2, 1 ))
      p3.add( p1 )
      p3.add( p2 )
      cp.add( p3, BorderLayout.CENTER  )

      f.setLocationRelativeTo( null )
      f.pack()
      f.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE )
      f.setVisible( true )
   }
}