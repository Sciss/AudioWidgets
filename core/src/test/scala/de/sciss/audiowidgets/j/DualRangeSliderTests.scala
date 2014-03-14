package de.sciss.audiowidgets.j

import java.awt.{BorderLayout, EventQueue}
import javax.swing.{BoxLayout, JButton, BorderFactory, WindowConstants, JFrame}
import de.sciss.audiowidgets.DualRangeModel
import javax.swing.event.{ChangeEvent, ChangeListener}

object DualRangeSliderTests extends App with Runnable {
  EventQueue.invokeLater(this)

  def run(): Unit = {
    val f   = new JFrame("Dual Range Slider")
    val m   = DualRangeModel(0, 100)
    m.value = 12
    m.range = 48 -> 100

    def mkSlider() = {
      val res = new DualRangeSlider(m)
      res.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4))
      res
    }

    val sl = mkSlider()

    val sl1 = mkSlider()
    sl1.valueVisible = false
    val sl2 = mkSlider()
    sl2.rangeVisible = false
    val sl3 = mkSlider()
    sl3.valueEditable = false
    val sl4 = mkSlider()
    sl4.rangeEditable = false

    // val p = Box.createVerticalBox()
    val p = new LCDPanel
    p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS))
    p.add(sl)
    p.add(sl1)
    p.add(sl2)
    p.add(sl3)
    p.add(sl4)

    f.getContentPane.add(p, BorderLayout.CENTER)
    f.getContentPane.add(new JButton("foo"), BorderLayout.SOUTH)
    f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    f.pack()
    f.setLocationRelativeTo(null)
    f.setVisible(true)

    sl.addChangeListener(new ChangeListener {
      def stateChanged(e: ChangeEvent): Unit =
        println(s"Change: value is ${m.value}, range is ${m.range}, adjusting? ${m.adjusting}")
    })
  }
}
