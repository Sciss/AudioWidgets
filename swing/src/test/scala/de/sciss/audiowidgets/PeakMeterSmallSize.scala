package de.sciss.audiowidgets

import scala.swing.Swing._
import scala.swing.event.ButtonClicked
import scala.swing.{BorderPanel, CheckBox, Frame, MainFrame, Orientation, SwingApplication}

object PeakMeterSmallSize extends SwingApplication {
  def mkMeter(orient: Orientation.Value): PeakMeter = {
    val m = new PeakMeter
    m.orientation = orient
    m.numChannels = 8
    m.update(Vector.fill(8)(List(0.8f, 0.4f)).flatten)
    println(s"INSETS = ${m.peer.getInsets}")
    val sz = (400, 8 * 4 + 2)
    m.preferredSize = if (orient == Orientation.Horizontal) sz else sz.swap
    m.minimumSize   = m.preferredSize
    m
  }

  def mkFrame(orient: Orientation.Value): Frame = {
    val m = mkMeter(orient)
    val t = new CheckBox("Border") {
      listenTo(this)
      reactions += {
        case ButtonClicked(_) => m.borderVisible = selected
      }
    }
    new MainFrame {
      contents = new BorderPanel {
        add(m, BorderPanel.Position.Center)
        add(t, BorderPanel.Position.East)
      }
      pack().centerOnScreen()
      open()
    }
  }

  def startup(args: Array[String]): Unit = {
    mkFrame(Orientation.Horizontal)
    mkFrame(Orientation.Vertical  )
  }
}
