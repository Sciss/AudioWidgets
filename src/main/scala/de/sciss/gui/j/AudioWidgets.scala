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
   val isSnapshot    = false

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
         (Some( new Color( 205, 232, 254 )), Some( new Color( 15, 42, 64 ))),
         (Some( Color.darkGray ), Some( Color.lightGray )),
//         (Some( new Color( 0xE0, 0xE0, 0xE0 )), Some( Color.darkGray )),
         (Some( new Color( 60, 30, 20 )), Some( new Color( 200, 100, 100 ))),
         (Some( new Color( 0xE0, 0xE0, 0xE0 )), Some( new Color( 0x20, 0x20, 0x20 ))))
      val lcdGrid       = new JPanel( new GridLayout( lcdColors.size, 1, 0, 6 ))
      val lb1 = lcdColors.zipWithIndex.map({ case ((fg, bg), idx) =>
         val lcd        = new LCDPanel
         bg.foreach( lcd.setBackground( _ ))
         val lb         = new JLabel( "00:00:0" + idx )
         lb.putClientProperty( "JComponent.sizeVariant", "small" )
         fg.foreach( lb.setForeground( _ ))
         lcd.add( lb )
         lcdGrid.add( lcd )
         lb
      }).head
      p.add( m, BorderLayout.WEST )
      p.add( Box.createHorizontalStrut( 20 ), BorderLayout.CENTER )
      val p2            = new JPanel( new BorderLayout() )
      p2.add( lcdGrid, BorderLayout.NORTH )

      val axis       = new Axis
      axis.format    = Axis.Format.Time()
      axis.minimum   = 0.0
      axis.maximum   = 34.56

      p.add( p2, BorderLayout.EAST )
      cp.add( p, BorderLayout.CENTER )
      cp.add( axis, BorderLayout.NORTH )

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
      val t2  = new Timer( 1000, new ActionListener {
         var cnt  = 0
         def actionPerformed( e: ActionEvent ) {
            cnt += 1
            val secs    = cnt % 60
            val mins    = (cnt / 60) % 60
            val hours   = (cnt / 3600) % 100
            lb1.setText( (hours + 100).toString.substring( 1 ) + ":" +
                         (mins + 100).toString.substring( 1 ) + ":" +
                         (secs + 100).toString.substring( 1 ))
         }
      })
      f.addWindowListener( new WindowAdapter {
         override def windowOpened( e: WindowEvent ) {
            t.start(); t2.start()
         }
         override def windowClosing( e: WindowEvent ) {
            t.stop(); t2.stop()
         }
      })

      f.setVisible( true )
   }
}