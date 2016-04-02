package de.sciss.audiowidgets.j

import java.awt.{BorderLayout, EventQueue}
import javax.swing.event.{ChangeEvent, ChangeListener}
import javax.swing.{BorderFactory, DefaultBoundedRangeModel, JButton, JFrame, WindowConstants}

import de.sciss.submin.Submin

object RotaryTests extends App with Runnable {
  Submin.install(args.contains("--dark"))
  EventQueue.invokeLater(this)

  def run(): Unit = {
    val f   = new JFrame("RotaryKnob")
    val m   = new DefaultBoundedRangeModel(50, 0, 0, 100)

    def mkSlider() = {
      val res = new RotaryKnob(m)
      res.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4))
      res
    }

    val sl = mkSlider()

    f.getContentPane.add(sl, BorderLayout.CENTER)
    f.getContentPane.add(new JButton("Foo"), BorderLayout.SOUTH)
    f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    f.pack()
    f.setLocationRelativeTo(null)
    f.setVisible(true)

    sl.addChangeListener(new ChangeListener {
      def stateChanged(e: ChangeEvent): Unit =
        println(s"Change: value is ${m.getValue}, adjusting? ${m.getValueIsAdjusting}")
    })
  }
}