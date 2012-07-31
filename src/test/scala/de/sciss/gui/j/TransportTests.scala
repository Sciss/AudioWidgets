package de.sciss.gui.j

import java.awt.{Paint, BorderLayout, Dimension, Component, LinearGradientPaint, RenderingHints, Color, BasicStroke, Shape, Graphics2D, Graphics, EventQueue}
import javax.swing.{BoxLayout, AbstractButton, JComponent, JButton, Icon, Box, WindowConstants, JPanel, JFrame}
import java.awt.geom.{RoundRectangle2D, Ellipse2D, AffineTransform, Rectangle2D, Area, GeneralPath}
import collection.immutable.{IndexedSeq => IIdxSeq}
import java.awt.event.{ActionListener, ActionEvent}

object TransportTests extends App with Runnable {
   EventQueue.invokeLater( this )

   def run() {
      val f = new JFrame()
      val p = new JComponent {
         setPreferredSize( new Dimension( 260, 70 ))
         override def paintComponent( g: Graphics ) {
            import TransportIcon.{paint => pnt, _}
            val g2 = g.asInstanceOf[ Graphics2D ]
            g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON )

            def gaga( scheme: ColorScheme, yoff: Int ) {
               Seq( GoToBegin, Rewind, Play, Stop, Pause, FastForward, GoToEnd, Record, Loop ).zipWithIndex.foreach {
                  case (icn, idx) =>
                     pnt( g2, icn, 10 + icn.defaultXOffset + (idx * 30), yoff + icn.defaultYOffset, 1f, scheme )
               }
            }
            gaga( DarkScheme, 10 )
            gaga( LightScheme, 40 )
         }
      }

      val sq = IndexedSeq( TransportIcon.GoToBegin, TransportIcon.Rewind, TransportIcon.Play, TransportIcon.Stop,
         TransportIcon.FastForward, TransportIcon.GoToEnd, TransportIcon.Loop )

      val butP = new JPanel( new BorderLayout() )
      def mkStrip( scheme: TransportIcon.ColorScheme, layPos: String ) {
         val strip = TransportIcon.makeButtonStrip( sq, scheme )
         strip.buttons.foreach { b =>
            b.addActionListener( new ActionListener {
               def actionPerformed( e: ActionEvent ) {
                  strip.iconType( b ).foreach { tp =>
                     f.setTitle( tp.toString )
                     if( tp == TransportIcon.Loop ) b.setSelected( !b.isSelected )
                  }
               }
            })
         }
         butP.add( strip, layPos )
      }
      mkStrip( TransportIcon.DarkScheme,  BorderLayout.NORTH )
      mkStrip( TransportIcon.LightScheme, BorderLayout.SOUTH )

      val cp = f.getContentPane
      cp.add( p, BorderLayout.NORTH )
      cp.add( butP, BorderLayout.SOUTH )

      f.pack() // f.setSize( 300, 200 )
      f.setResizable( false )
      f.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE )
      f.setVisible( true )
   }
}
