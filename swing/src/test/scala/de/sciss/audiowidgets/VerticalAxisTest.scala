package de.sciss.audiowidgets

import scala.swing.event.UIElementResized
import scala.swing.{Frame, MainFrame, Orientation, SimpleSwingApplication}

object VerticalAxisTest extends SimpleSwingApplication {
//  override def main(args: Array[String]): Unit = {
//    val res = AxisFormat.Decimal.format(1.1, 2, 0)
//    println(res)
//  }

  lazy val top: Frame = new MainFrame {
    contents = new Axis(Orientation.Vertical) {
      maximum = 1
//      fixedBounds = true
      minimumSize = {
        val d = minimumSize
        // in metal laf, the bug occurs with height greater than or equal to this
        d.height = math.max(735, d.height)
        d
      }
      preferredSize = minimumSize
      listenTo(this)
      reactions += {
        case UIElementResized(_) => println(s"height = ${peer.getHeight}")
      }
    }
    pack().centerOnScreen()
  }
}
