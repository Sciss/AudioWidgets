package de.sciss.audiowidgets

import java.awt.Graphics2D

import de.sciss.audiowidgets.impl.TimelineCanvasImpl
import de.sciss.desktop.impl.{SwingApplicationImpl, WindowImpl}
import de.sciss.desktop.{Menu, Window, WindowHandler}
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
      def handler: WindowHandler = App.windowHandler

      title = "Application Demo"

      val canvas: TimelineCanvas = new TimelineCanvasImpl {
        /** The underlying model */
        val timelineModel: TimelineModel = {
          val span0 = Span(0, (sampleRate * 60).toLong)
          TimelineModel(bounds = span0, visible = span0, sampleRate = sampleRate, clipStop = false)
        }

        /** The corresponding Swing component */
        lazy val canvasComponent: Component = new Component {
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