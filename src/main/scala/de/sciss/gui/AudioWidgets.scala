package de.sciss.gui

import java.awt.{EventQueue, BorderLayout}
import javax.swing.{Timer, JFrame, WindowConstants}
import java.awt.event.{WindowEvent, WindowAdapter, ActionEvent, ActionListener}

object AudioWidgets extends App with Runnable {
   EventQueue.invokeLater( this )

   def run() {
      val f    = new JFrame( "AudioWidgets" )
      val cp   = f.getContentPane
      val m    = new PeakMeter
      m.ticks  = 60
      cp.add( m, BorderLayout.WEST )
      f.pack()
      f.setLocationRelativeTo( null )
      f.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE )

      val t    = new Timer( 30, new ActionListener {
         val rnd  = new util.Random()
         var peak = 0.5f
         var rms  = 0f
         def actionPerformed( e: ActionEvent ) {
            peak = math.max( 0f, math.min( 1f, peak + math.pow( rnd.nextFloat() * 0.5, 2 ).toFloat * (if( rnd.nextBoolean() ) 1 else -1) ))
            rms  = math.max( 0f, math.min( peak, rms * 0.98f + (rnd.nextFloat() * 0.02f * (if( rnd.nextBoolean() ) 1 else -1) )))
            m.setPeakAndRMS( peak, rms )
         }
      })
      f.addWindowListener( new WindowAdapter {
         override def windowOpened( e: WindowEvent ) {
            t.start()
         }
         override def windowClosing( e: WindowEvent ) {
            t.stop()
         }
      })

      f.setVisible( true )
   }
}