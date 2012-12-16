package de.sciss.gui.j

import java.awt.{Color, BorderLayout, RenderingHints, Graphics2D, Graphics, Dimension, EventQueue}
import javax.swing.{WindowConstants, JFrame, JComponent}

object WaveTests extends App with Runnable {
   EventQueue.invokeLater( this )

   def run() {
      val dataLen = 64
      val freq    = 2 * math.Pi / dataLen
      val data    = Array.tabulate( dataLen ) { i => math.sin( i * freq ).toFloat }

      val pnt              = WavePainter.sampleAndHold
//      pnt.zoomX.sourceLow  = 0
      pnt.zoomX.sourceHigh = dataLen
      pnt.zoomY.sourceLow  = -1
      pnt.zoomY.sourceHigh = 1
//      pnt.zoomX.targetLow  = 0
      pnt.zoomY.targetHigh = 0
      pnt.color = Color.white

      val p = new JComponent {
         setPreferredSize( new Dimension( 260, 70 ))
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

      val f    = new JFrame()
      val cp   = f.getContentPane
      cp.add( p, BorderLayout.CENTER )

      f.setLocationRelativeTo( null )
      f.pack()
      f.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE )
      f.setVisible( true )
   }
}