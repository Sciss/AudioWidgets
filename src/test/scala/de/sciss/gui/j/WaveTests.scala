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
      val dataDecim  = {
         val factor  = 32 // 16
         val fullLen = dataLen * factor
         val halfLen = fullLen / 2
         val full    = Array.tabulate( fullLen ) { i =>
            val j = i - halfLen
            val k = j * freq * 8/factor
            (math.sin( k ) / k).toFloat
         }
         val res = new Array[ Float ]( dataLen * 3 )
         WavePainter.Decimator.pcmToPeakRMS( factor ).decimate( full, 0, res, 0, dataLen )
         res
      }

      val pntSH      = WavePainter.sampleAndHold
      val pntLin     = WavePainter.linear
      val pntPeakRMS = WavePainter.peakRMS

      val pntAll  = pntLin :: pntSH :: pntPeakRMS :: Nil

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
      val ggPnt   = new JComboBox( Array[ AnyRef ]( "Linear", "Sample+Hold", "Peak+RMS" ))
      panel.add( ggPnt )
      ggPnt.addActionListener( new ActionListener {
         def actionPerformed( e: ActionEvent ) {
            pnt   = pntAll( ggPnt.getSelectedIndex )
            data  = if( pnt == pntPeakRMS ) dataDecim else dataFull
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