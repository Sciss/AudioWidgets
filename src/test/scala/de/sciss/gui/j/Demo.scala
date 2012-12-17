package de.sciss.gui.j

import java.awt.event.{WindowEvent, WindowAdapter, ActionEvent, ActionListener}
import collection.immutable.{IndexedSeq => IIdxSeq}
import javax.swing.{JComponent, Box, JLabel, BorderFactory, JFrame, JPanel, Timer, WindowConstants}
import java.awt.{Color, GridLayout, EventQueue, BorderLayout}

object Demo extends App with Runnable {
   EventQueue.invokeLater( this )

   def run() {
      val f             = new JFrame( "AudioWidgets" )
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

      lazy val trnspActions = Seq(
         Transport.GoToBegin, Transport.Play, Transport.Stop, Transport.GoToEnd, Transport.Loop ).map {
         case l @ Transport.Loop => l.apply { trnsp.button( l ).foreach( b => b.setSelected( !b.isSelected ))}
         case e => e.apply {}
      }
      lazy val trnsp: JComponent with Transport.ButtonStrip = Transport.makeButtonStrip( trnspActions )

      p.add( p2, BorderLayout.EAST )
      cp.add( p, BorderLayout.CENTER )
      cp.add( axis, BorderLayout.NORTH )
      cp.add( trnsp, BorderLayout.SOUTH )

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