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

import java.awt.event.{WindowEvent, WindowAdapter, ActionEvent, ActionListener}
import collection.immutable.{IndexedSeq => IIdxSeq}
import javax.swing.{Box, JLabel, BorderFactory, JFrame, JPanel, Timer, WindowConstants}
import java.awt.{Color, GridLayout, EventQueue, BorderLayout}

object AudioWidgets extends App with Runnable {
   val name          = "AudioWidgets"
   val version       = 0.10
   val copyright     = "(C)opyright 2011 Hanns Holger Rutz"
   val isSnapshot    = true

   EventQueue.invokeLater( this )

   def versionString = {
      val s = (version + 0.001).toString.substring( 0, 4 )
      if( isSnapshot ) s + "-SNAPSHOT" else s
   }

   def run() {
      val f             = new JFrame( name )
      f.getRootPane.putClientProperty( "apple.awt.brushMetalLook", java.lang.Boolean.TRUE )
      val cp            = f.getContentPane
      val p             = new JPanel( new BorderLayout() )
      p.setBorder( BorderFactory.createEmptyBorder( 20, 20, 20, 20 ))

      val m             = new PeakMeter()
      m.numChannels     = 1
      m.hasCaption      = true
      m.borderVisible   = true

      val lcdColors     = IndexedSeq(
         (Some( Color.darkGray /* new Color( 0x40, 0x40, 0x40 ) */), None),
         (Some( Color.white ), Some( new Color( 15, 42, 64 ))),
         (Some( Color.darkGray ), Some( Color.lightGray )),
         (Some( Color.lightGray), Some( Color.darkGray)))
      val lcdGrid       = new JPanel( new GridLayout( lcdColors.size, 1, 0, 4 ))
      lcdColors.zipWithIndex.foreach { case ((fg, bg), idx) =>
         val lcd        = new LCDPanel
         bg.foreach( lcd.setBackground( _ ))
         val lb         = new JLabel( "00:00:0" + idx )
         lb.putClientProperty( "JComponent.sizeVariant", "small" )
         fg.foreach( lb.setForeground( _ ))
         lcd.add( lb )
         lcdGrid.add( lcd )
      }
      p.add( m, BorderLayout.WEST )
      p.add( Box.createHorizontalStrut( 20 ), BorderLayout.CENTER )
      val p2            = new JPanel( new BorderLayout() )
      p2.add( lcdGrid, BorderLayout.NORTH )

      p.add( p2, BorderLayout.EAST )
      cp.add( p, BorderLayout.CENTER )
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
            m.update( IIdxSeq( peak, rms ))
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