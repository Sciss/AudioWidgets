package de.sciss.audiowidgets.j

import java.awt.EventQueue
import javax.swing.{WindowConstants, JFrame}
import de.sciss.audiowidgets.DualRangeModel

object DualRangeSliderTests extends App with Runnable {
  EventQueue.invokeLater(this)

  def run(): Unit = {
    val f   = new JFrame("Dual Range Slider")
    val m   = DualRangeModel(0, 100)
    m.value = 12
    m.range = 48 -> 100
    val sl  = new DualRangeSlider(m)
    f.getContentPane.add(sl)
    f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    f.pack()
    f.setLocationRelativeTo(null)
    f.setVisible(true)
  }
}
