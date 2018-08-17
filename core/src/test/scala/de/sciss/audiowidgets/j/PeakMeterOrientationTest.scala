package de.sciss.audiowidgets
package j

import java.awt.{BorderLayout, EventQueue}

import de.sciss.submin.Submin
import javax.swing._

object PeakMeterOrientationTest extends App with Runnable {
  Submin.install(true)
  EventQueue.invokeLater(this)

  def run(): Unit = {
    Seq(SwingConstants.HORIZONTAL, SwingConstants.VERTICAL).foreach { orient =>
      val f = new JFrame("AudioWidgets")
      f.getRootPane.putClientProperty("apple.awt.brushMetalLook", java.lang.Boolean.TRUE)
      val cp = f.getContentPane
      val p = new JPanel(new BorderLayout())
      p.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20))

      val m = new PeakMeter()
      m.orientation   = orient
      m.numChannels   = 1
      m.caption       = true
      m.borderVisible = true

      p .add(m, BorderLayout.CENTER)
      cp.add(p, BorderLayout.CENTER)

      f.pack()
      f.setLocationRelativeTo(null)
      f.setLocation(f.getX + f.getWidth/2 * (if (orient == SwingConstants.HORIZONTAL) 1 else -1), f.getY)

      f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)

      f.setVisible(true)
    }
  }
}