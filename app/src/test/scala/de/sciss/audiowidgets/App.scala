package de.sciss.audiowidgets

import java.awt.Graphics2D

import de.sciss.audiowidgets.impl.TimelineCanvasImpl
import de.sciss.desktop.impl.{SwingApplicationImpl, WindowImpl}
import de.sciss.desktop.{Menu, Window}
import de.sciss.span.Span
import de.sciss.submin.Submin
import de.sciss.swingplus.Implicits._

import scala.swing.Component
import scala.swing.Swing._

object App extends SwingApplicationImpl("AudioWidgets") {
  protected lazy val menuFactory = Menu.Root()
  type Document = Unit

  def sampleRate = 44100

  override protected def init(): Unit = {
    val isDark = args.contains("--dark")
    Submin.install(isDark)

    new WindowImpl {
      def handler = App.windowHandler

      title = "Application Demo"

      // println("CREATE WIN")
      val canvas = new TimelineCanvasImpl {
        // println("INIT SUB")
        /** The underlying model */
        val timelineModel = TimelineModel(Span(0, (sampleRate * 60).toLong), sampleRate)

        /** The corresponding Swing component */
        lazy val canvasComponent = new Component {
          preferredSize = (800, 400)

          override protected def paintComponent(g: Graphics2D): Unit = {
            g.setPaint(TimelineCanvasImpl.pntChecker)
            val w = this.width
            val h = this.height
            g.fillRect(0, 0, w, h)
            paintPosAndSelection(g, h)
          }
        }
      }

      contents = canvas.component

      closeOperation = Window.CloseExit
      pack()
      front()
    }
  }
}