package de.sciss.gui.j

import java.awt.{FlowLayout, Color, BorderLayout, RenderingHints, Graphics2D, Graphics, Dimension, EventQueue}
import javax.swing.{JComboBox, JPanel, WindowConstants, JFrame, JComponent}
import java.awt.event.{ActionListener, ActionEvent, ItemEvent, ItemListener}

object WaveTests extends App with Runnable {
   EventQueue.invokeLater( this )

   def run() {
      val dataLen = 64
      val freq    = 2 * math.Pi / (dataLen - 1)
      val data    = Array.tabulate( dataLen ) { i => math.sin( i * freq ).toFloat }

      val pntSH   = WavePainter.sampleAndHold
      val pntLin  = WavePainter.linear

      val pntAll  = pntLin :: pntSH :: Nil

//      pnt.zoomX.sourceLow  = 0

      pntAll.foreach { pnt =>
         pnt.zoomX.sourceHigh = dataLen - 1
         pnt.zoomY.sourceLow  = -1
         pnt.zoomY.sourceHigh = 1
   //      pnt.zoomX.targetLow  = 0
         pnt.zoomY.targetHigh = 0
         pnt.color = Color.white
      }

      var pnt: WavePainter = pntLin

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
      val ggPnt   = new JComboBox( Array[ AnyRef ]( "Linear", "Sample+Hold" ))
      panel.add( ggPnt )
      ggPnt.addActionListener( new ActionListener {
         def actionPerformed( e: ActionEvent ) {
            pnt = pntAll( ggPnt.getSelectedIndex )
//println( "Alora " + pnt )
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