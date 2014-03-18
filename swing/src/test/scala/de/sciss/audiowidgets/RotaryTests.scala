package de.sciss.audiowidgets

import javax.swing.{DefaultBoundedRangeModel, BorderFactory}
import scala.swing.{Button, BorderPanel, MainFrame, Frame, SimpleSwingApplication}
import scala.swing.event.ValueChanged

object RotaryTests extends SimpleSwingApplication {
  lazy val top: Frame = {
    val m   = new DefaultBoundedRangeModel(50, 0, 0, 100)

    def mkSlider() = {
      val res     = new RotaryKnob(m)
      res.border  = BorderFactory.createEmptyBorder(4, 4, 4, 4)
      res
    }

    val sl = mkSlider()
    sl.listenTo(sl)
    sl.reactions += {
      case ValueChanged(_) =>
        println(s"Change: value is ${sl.value}, adjusting? ${sl.adjusting}")
    }

    new MainFrame {
      contents = new BorderPanel {
        add(sl, BorderPanel.Position.Center)
        add(new Button("Foo"), BorderPanel.Position.South)
      }
    }
  }
}