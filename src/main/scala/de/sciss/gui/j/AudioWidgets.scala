/*
 *  AudioWidgets.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011 Hanns Holger Rutz. All rights reserved.
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

import java.awt.{EventQueue, BorderLayout}
import javax.swing.{Timer, JFrame, WindowConstants}
import java.awt.event.{WindowEvent, WindowAdapter, ActionEvent, ActionListener}

object AudioWidgets extends App with Runnable {
   val name          = "AudioWidgets"
   val version       = 0.10
   val copyright     = "(C)opyright 2011 Hanns Holger Rutz"
   var isSnapshot    = false

   EventQueue.invokeLater( this )

   def versionString = {
      val s = (version + 0.001).toString.substring( 0, 4 )
      if( isSnapshot ) s + "-SNAPSHOT" else s
   }

   def run() {
      val f    = new JFrame( name )
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