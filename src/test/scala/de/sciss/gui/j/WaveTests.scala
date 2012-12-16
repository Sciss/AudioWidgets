package de.sciss.gui.j

import java.awt.{FlowLayout, Color, BorderLayout, RenderingHints, Graphics2D, Graphics, Dimension, EventQueue}
import javax.swing.{JComboBox, JPanel, WindowConstants, JFrame, JComponent}
import java.awt.event.{ActionListener, ActionEvent, ItemEvent, ItemListener}

object WaveTests extends App with Runnable {
   EventQueue.invokeLater( this )

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

      val p = new JComponent {
         setPreferredSize( new Dimension( 260, 140 ))
         override def paintComponent( g: Graphics ) {
            val g2 = g.asInstanceOf[ Graphics2D ]
            g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON )
            g2.setRenderingHint( RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE  )
            g2.setColor( Color.black )
            val w = getWidth
            val h = getHeight
            g2.fillRect( 0, 0, w, h )
            pnt.zoomX.targetHigh = w
            pnt.zoomY.targetLow  = h - 1
            pnt.paint( g2, data, 0, dataLen )
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
            p.repaint()
         }
      })

      val f    = new JFrame()
      val cp   = f.getContentPane
      cp.add( p, BorderLayout.CENTER )
      cp.add( panel, BorderLayout.NORTH )

      f.setLocationRelativeTo( null )
      f.pack()
      f.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE )
      f.setVisible( true )
   }
}