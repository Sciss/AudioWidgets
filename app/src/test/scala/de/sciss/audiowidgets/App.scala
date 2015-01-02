package de.sciss.audiowidgets

import com.alee.laf.WebLookAndFeel
import de.sciss.desktop.impl.{WindowImpl, SwingApplicationImpl}
import de.sciss.desktop.{Window, Menu}
import de.sciss.audiowidgets.impl.TimelineCanvasImpl
import scala.swing.{Swing, Component}
import java.awt.Graphics2D
import de.sciss.span.Span
import Swing._
import de.sciss.swingplus.Implicits._

object App extends SwingApplicationImpl("AudioWidgets") {
  protected lazy val menuFactory = Menu.Root()
  type Document = Unit

  def sampleRate = 44100

  override protected def init(): Unit = {
    WebLookAndFeel.install()
    new WindowImpl {
      def handler = App.windowHandler

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