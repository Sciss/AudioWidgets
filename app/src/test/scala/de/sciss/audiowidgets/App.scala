package de.sciss.audiowidgets

import java.awt.{Color, Graphics2D}

import de.sciss.audiowidgets.impl.TimelineCanvasImpl
import de.sciss.desktop.impl.{SwingApplicationImpl, WindowImpl}
import de.sciss.desktop.{Menu, Window, WindowHandler}
import de.sciss.span.Span
import de.sciss.submin.Submin
import de.sciss.swingplus.Implicits._

import scala.swing.Component
import scala.swing.Swing._

/** Shows the `TimelineCanvas` with a virtual span of 20 to 60 seconds,
  * and an object placed in the span 24 to 40 seconds.
  */
object App extends SwingApplicationImpl("AudioWidgets") {
  protected lazy val menuFactory: Menu.Root = Menu.Root()
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
          val span0 = Span((sampleRate * 20).toLong, (sampleRate * 60).toLong)
          TimelineModel(bounds = span0, visible = span0, virtual = span0, sampleRate = sampleRate, clipStop = false)
        }

        timelineModel.addListener {
          case TimelineModel.Position (_, p)   => println(s"position  = $p")
          case TimelineModel.Visible  (_, sp)  => println(s"visible   = $sp")
          case TimelineModel.Selection(_, sp)  => println(s"selection = $sp")
          case TimelineModel.Virtual  (_, sp)  => println(s"virtual   = $sp")
          case TimelineModel.Bounds   (_, sp)  => println(s"bounds    = $sp")
        }

        val objSpan: Span = Span((sampleRate * 24).toLong, (sampleRate * 40).toLong)

        protected def transportRunning  : Boolean = false
        protected def transportPause  (): Unit    = ()
        protected def transportResume (): Unit    = ()

        /** The corresponding Swing component */
        lazy val canvasComponent: Component = new Component {
          preferredSize = (800, 400)

          override protected def paintComponent(g: Graphics2D): Unit = {
            g.setPaint(TimelineCanvasImpl.pntChecker)
            val w = this.width
            val h = this.height
            g.fillRect(0, 0, w, h)
            val x1 = frameToScreen(objSpan.start).toInt
            val x2 = frameToScreen(objSpan.stop ).toInt
            g.setColor(if (objSpan.contains(timelineModel.position)) Color.red else Color.orange)
            g.fillRect(x1, 10, x2 - x1, 100)
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