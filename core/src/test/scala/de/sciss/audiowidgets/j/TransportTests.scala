package de.sciss.audiowidgets.j

import java.awt.{BorderLayout, Dimension, EventQueue, Graphics, Graphics2D, RenderingHints}
import javax.swing.{JComponent, JFrame, JPanel, WindowConstants}

import de.sciss.submin.Submin

object TransportTests extends App with Runnable {
  Submin.install(args.contains("--dark"))
  EventQueue.invokeLater(this)

  def run(): Unit = {
    val f = new JFrame()
    val p = new JComponent {
      setPreferredSize(new Dimension(280, 70))

      override def paintComponent(g: Graphics): Unit = {
        import Transport.{paint => pnt, _}
        val g2 = g.asInstanceOf[Graphics2D]
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        def gaga(scheme: ColorScheme, yoff: Int): Unit =
          Seq(GoToBegin, Rewind, Stop, Pause, Play, FastForward, GoToEnd, Record, Loop).zipWithIndex.foreach {
            case (icn, idx) =>
              pnt(g2, icn, 10 + icn.defaultXOffset + (idx * 30), yoff + icn.defaultYOffset, 1f, scheme)
          }

        gaga(DarkScheme, 10)
        gaga(LightScheme, 40)
      }
    }

    val sq = IndexedSeq(Transport.GoToBegin, Transport.Rewind, Transport.Stop,
      Transport.Pause, Transport.Play, Transport.FastForward, Transport.GoToEnd, Transport.Record, Transport.Loop)

    val butP = new JPanel(new BorderLayout())
    def mkStrip(scheme: Transport.ColorScheme, layPos: String): Unit = {
      lazy val act = sq.map { e =>
        e {
          f.setTitle(e.toString)
          if (e == Transport.Loop) strip.button(e).foreach { b =>
            b.setSelected(!b.isSelected)
          }
        }
      }
      lazy val strip: JComponent with Transport.ButtonStrip = Transport.makeButtonStrip(act, scheme = scheme)
      //         strip.buttons.foreach { b =>
      //            b.addActionListener( new ActionListener {
      //               def actionPerformed( e: ActionEvent ) {
      //                  strip.element( b ).foreach { tp =>
      //                     f.setTitle( tp.toString )
      //                     if( tp == Transport.Loop ) b.setSelected( !b.isSelected )
      //                  }
      //               }
      //            })
      //         }
      butP.add(strip, layPos)
    }
    mkStrip(Transport.DarkScheme , BorderLayout.NORTH)
    mkStrip(Transport.LightScheme, BorderLayout.SOUTH)

      val cp = f.getContentPane
      cp.add( p, BorderLayout.NORTH )
      cp.add( butP, BorderLayout.SOUTH )

      f.pack() // f.setSize( 300, 200 )
      f.setResizable( false )
      f.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE )
      f.setVisible( true )
   }
}
