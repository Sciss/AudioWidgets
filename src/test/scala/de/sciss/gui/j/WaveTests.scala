package de.sciss.gui.j

import java.awt.{GridLayout, Rectangle, FlowLayout, Color, BorderLayout, RenderingHints, Graphics2D, Graphics, Dimension, EventQueue}
import javax.swing.{SwingConstants, JSlider, JComboBox, JPanel, WindowConstants, JFrame, JComponent}
import java.awt.event.{ActionListener, ActionEvent}
import de.sciss.gui.j.WavePainter.MultiResolution
import collection.immutable.{IndexedSeq => IIdxSeq}
import javax.swing.event.{ChangeEvent, ChangeListener}

object WaveTests extends App with Runnable {
   EventQueue.invokeLater( this )

   private def ??? = sys.error( "TODO" )

   def run() {
      val dataLen    = 128 // 64
      val freq       = 2 * math.Pi / (dataLen - 1)
      val dataFull   = Array.tabulate( dataLen ) { i => math.sin( i * freq ).toFloat }

      def sincDecim( over: Int ) = {
         val factor  = 256 // 32 // 16
         val dl2     = dataLen * over
         val fullLen = dl2 * factor
         val halfLen = fullLen / 2
         var a0 = math.random
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
         pnt.zoomX.sourceHigh = dataLen - 1
         pnt.zoomY.sourceLow  = -1
         pnt.zoomY.sourceHigh = 1
   //      pnt.zoomX.targetLow  = 0
         pnt.zoomY.targetHigh = 0
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
            pnt.zoomX.targetHigh = w
            pnt.zoomY.targetLow  = h - 1
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

      val multiSize  = 131072
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

      final class SimpleReader( data: Array[ Float ], val decimationFactor: Int ) extends MultiResolution.Reader {
         private val isFull = decimationFactor == 1
         def tupleSize = if( isFull ) 1 else 3
         def available( srcOff: Long, len: Int ) : IIdxSeq[ Int ] = IIdxSeq( 0, len )

         def read( buf: Array[ Array[ Float ]], bufOff: Int, srcOff: Long, len : Int ) : Boolean = {
            val bch  = buf( 0 )
            var i    = bufOff * tupleSize    // if( isFull ) bufOff else bufOff * 3
            val stop = i + len * tupleSize  // (if( isFull ) len * 3 else len)
            var j = 0; while( i < stop ) {
               bch( i ) = data( j )
            i += 1; j += 1 }
            true
         }
      }

      val fullReader    = new SimpleReader( fullData, 1 )
      val decimData1    = new Array[ Float ]( multiSize / 32 * 3 )
      WavePainter.Decimator.pcmToPeakRMS( 32 ).decimate( fullData, 0, decimData1, 0, multiSize / 32 )
      val decim1Reader  = new SimpleReader( decimData1, 32 )
      val decimData2    = new Array[ Float ]( multiSize / (32*32) * 3 )
      WavePainter.Decimator.peakRMS( 32 ).decimate( decimData1, 0, decimData2, 0, multiSize / (32*32) )
      val decim2Reader  = new SimpleReader( decimData2, 32*32 )

      val mSrc = new MultiResolution.Source {
         def numChannels : Int = 1
         def numFrames : Long = multiSize

         val readers : IIdxSeq[ MultiResolution.Reader ] = IIdxSeq( fullReader, decim1Reader, decim2Reader )

//         def decimationFactor : Int = 1
//
//         def available( srcOff: Long, len: Int ) : IIdxSeq[ Int ] = IIdxSeq( 0, len )
//
//         def read( buf: Array[ Array[ Float ]], bufOff: Int, srcOff: Long, len : Int ) : Boolean = {
//            val bch = buf( 0 )
//            val r = new util.Random( 0L )
//            var i = bufOff; val stop = i + len; while( i < stop ) {
//               bch( i ) = r.nextFloat() * 2 - 1
//            i += 1}
//            true
//         }
      }
      lazy val multi = WavePainter.MultiResolution( mSrc, place )
      multi.peakColor   = Color.gray
      multi.rmsColor    = Color.white

      lazy val ggZoomX = new JSlider( SwingConstants.HORIZONTAL, 0, 1000, 0 )
      ggZoomX.setPaintTicks( true )
      ggZoomX.putClientProperty( "JComponent.sizeVariant", "small" )
      lazy val refreshL: ChangeListener = new ChangeListener {
         def stateChanged( e: ChangeEvent ) {
            view2.repaint()
         }
      }
      ggZoomX.addChangeListener( refreshL )
      lazy val ggZoomY = new JSlider( SwingConstants.VERTICAL, 0, 1000, 500 )
      ggZoomY.setInverted( true )
      ggZoomY.setPaintTicks( true )
      ggZoomY.putClientProperty( "JComponent.sizeVariant", "small" )
      ggZoomY.addChangeListener( refreshL )

      implicit def richDouble( x: Double ) = new RichDouble( x )
      final class RichDouble( x: Double ) {
         def linlin( srcLo: Double, srcHi: Double, dstLo: Double, dstHi: Double ) =
            (x - srcLo) / (srcHi - srcLo) * (dstHi - dstLo) + dstLo

         def linexp( srcLo: Double, srcHi: Double, dstLo: Double, dstHi: Double) =
            math.pow( dstHi / dstLo, (x- srcLo) / (srcHi - srcLo) ) * dstLo
      }

      lazy val view2 = new SimpleView {
         def paint( g: Graphics2D, w: Int, h: Int ) {
            val vz = ggZoomY.getValue.linexp( 0, 1000, 1.0/8, 8.0 )
            val hz = ggZoomX.getValue.linexp( 0, 1000, 1.0, 1.0/800 )
            multi.zoomX.sourceHigh  = multiSize * hz
            multi.zoomX.targetHigh  = w
            multi.zoomY.sourceLow   = -1 * vz
            multi.zoomY.sourceHigh  = 1 * vz
            multi.zoomY.targetLow   = h - 1
            multi.zoomY.targetHigh  = 0
            multi.paint( g )
         }
      }
      lazy val place: MultiResolution.ChannelPlacement = new MultiResolution.ChannelPlacement {
         def rectangleForChannel( ch: Int, r: Rectangle ) {
            view2.getBounds( r ) // single channel
         }
      }

      val f    = new JFrame()
      val cp   = f.getContentPane

      val p1 = new JPanel( new BorderLayout() )
      p1.add( view1, BorderLayout.CENTER )
      p1.add( panel, BorderLayout.NORTH  )

      val p2 = new JPanel( new BorderLayout() )
      p2.add( view2, BorderLayout.CENTER )
      p2.add( ggZoomX, BorderLayout.SOUTH )
      p2.add( ggZoomY, BorderLayout.EAST )

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