/*
 *  TimelineCanvasImpl.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2014 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets
package impl

import java.awt.{Graphics2D, Rectangle, TexturePaint, Color}
import java.awt.image.BufferedImage
import de.sciss.span.Span
import scala.swing.event.{UIElementResized, ValueChanged, MouseDragged, Key, MousePressed}
import scala.swing.{Swing, BoxPanel, BorderPanel, Component, Reactions, Orientation}
import de.sciss.swingplus.ScrollBar
import de.sciss.model.Change
import de.sciss.desktop.impl.DynamicComponentImpl
import Swing._

object TimelineCanvasImpl {
  private sealed trait AxisMouseAction
  private case object AxisPosition extends AxisMouseAction
  private final case class AxisSelection(fix: Long) extends AxisMouseAction

  private val colrSelection     = new Color(0x00, 0x00, 0xFF, 0x4F)
  private val colrPositionXor   = Color.black // new Color(0x00, 0x00, 0xFF, 0x7F)
  private val colrPosition      = Color.white // new Color(0x00, 0x00, 0xFF, 0x7F)
  // private val colrSelection2    = new Color(0x00, 0x00, 0x00, 0x40)
  // private val colrPlayHead      = new Color(0x00, 0xD0, 0x00, 0xC0)

  private val imgChecker = {
 		val img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB)
    var x = 0
    while (x < 64) {
      var y = 0
      while (y < 64) {
        img.setRGB(x, y, if (((x / 32) ^ (y / 32)) == 0) 0xFF9F9F9F else 0xFF7F7F7F)
        y += 1
      }
      x += 1
    }
    img
  }

  // XXX TODO: this should go somewhere else
  final val pntChecker: TexturePaint = new TexturePaint(imgChecker, new Rectangle(0, 0, 64, 64))
}
trait TimelineCanvasImpl extends TimelineCanvas {
  view =>

  import TimelineCanvasImpl._

  private var axisMouseAction: AxisMouseAction = AxisPosition

  // this is an auxiliary object which may be used
  // by any method on the EDT
  private val r = new Rectangle

  protected def paintPosAndSelection(g: Graphics2D, h: Int) {
    val pos = frameToScreen(timelineModel.position).toInt
    g.getClipBounds(r)
    val rr  = r.x + r.width
    timelineModel.selection match {
      case Span(start, stop) =>
        val selx1 = frameToScreen(start).toInt
        val selx2 = frameToScreen(stop ).toInt
        if (selx1 < rr && selx2 > r.x) {
          g.setColor(colrSelection)
          g.fillRect(selx1, 0, selx2 - selx1, h)
          if (r.x <= selx1) g.drawLine(selx1, 0, selx1, h)
          if (selx2 > selx1 && rr >= selx2) g.drawLine(selx2 - 1, 0, selx2 - 1, h)
        }
      case _ =>
    }
    if (r.x <= pos && rr > pos) {
      g.setXORMode(colrPositionXor)
      g.setColor(colrPosition)
      g.drawLine(pos, 0, pos, h)
      g.setPaintMode()
    }
  }

  // lazy because of `timelineModel`
  private lazy val timeAxis = new Axis {
    override protected def paintComponent(g: Graphics2D) {
      super.paintComponent(g)
      paintPosAndSelection(g, peer.getHeight)
    }
    private val maxSecs = timelineModel.bounds.stop  / timelineModel.sampleRate
    format              = AxisFormat.Time(hours = maxSecs >= 3600.0, millis = true)

    listenTo(mouse.clicks)
    listenTo(mouse.moves)

    // ---- install actions if model is modifiable ----

    timelineModel.modifiableOption.foreach { m =>
      reactions += {
        case MousePressed(_, point, mod, _, _) =>
          // no mods: move position; shift: extend selection; alt: clear selection
          val frame = clipVisible(screenToFrame(point.x))
          if ((mod & Key.Modifier.Alt) != 0) {
            m.selection = Span.Void
          }
          if ((mod & Key.Modifier.Shift) != 0) {
            val otra = m.selection match {
              case Span.Void          => m.position
              case Span(start, stop)  => if (math.abs(frame - start) > math.abs(frame - stop)) start else stop
            }
            axisMouseAction = AxisSelection(otra)
          } else {
            axisMouseAction = AxisPosition
          }
          processAxisMouse(m, frame)

        case MouseDragged(_, point, _) =>
          val frame = clipVisible(screenToFrame(point.x))
          processAxisMouse(m, frame)
      }
    }
  }

  private def updateAxis() {
    val visi          = timelineModel.visible
    val sr            = timelineModel.sampleRate
    timeAxis.minimum  = visi.start / sr
    timeAxis.maximum  = visi.stop  / sr
  }

  // final def canvasComponent: Component

  private val scroll = new ScrollBar {
    orientation   = Orientation.Horizontal
    unitIncrement = 4
  }

  final def framesToScreen(numFrames: Long): Double = {
    val visi = timelineModel.visible
    numFrames.toDouble / visi.length * canvasComponent.peer.getWidth
  }

  final def frameToScreen(frame: Long): Double = {
    val visi = timelineModel.visible
    (frame - visi.start).toDouble / visi.length * canvasComponent.peer.getWidth
  }

  final def screenToFrame(screen: Int): Double = {
    val visi = timelineModel.visible
    screen.toDouble / canvasComponent.peer.getWidth * visi.length + visi.start
  }

  final def clipVisible(frame: Double): Long = {
    val visi = timelineModel.visible
    visi.clip(frame.toLong)
  }

  private def updateScroll() {
    val trackWidth      = math.max(1, scroll.peer.getWidth - 32)  // TODO XXX stupid hard coded value. but how to read it?
    val visi            = timelineModel.visible
    val total           = timelineModel.bounds
    val framesPerPixel  = math.max(1, ((total.length + (trackWidth >> 1)) / trackWidth).toInt)
    val max             = math.min(0x3FFFFFFFL, total.length / framesPerPixel).toInt
    val pos             = math.min(max - 1, (visi.start - total.start) / framesPerPixel).toInt
    val visiAmt         = math.min(max - pos, visi.length / framesPerPixel).toInt
    val blockInc        = math.max(1, visiAmt * 4 / 5)
    // val unitInc         = 4

    // __DO NOT USE deafTo and listenTo__ there must be a bug in scala-swing,
    // because that quickly overloads the AWT event multicaster with stack overflows.
    //    deafTo(scroll)
    val l = pane.isListeningP
    if (l) scroll.reactions -= scrollListener
    scroll.maximum        = max
    scroll.visibleAmount  = visiAmt
    scroll.value          = pos
    scroll.blockIncrement = blockInc
    //    listenTo(scroll)
    if (l) scroll.reactions += scrollListener
  }

  private def updateFromScroll(model: TimelineModel.Modifiable) {
    val visi              = model.visible
    val total             = model.bounds
    val pos               = math.min(total.stop - visi.length,
      ((scroll.value.toDouble / scroll.maximum) * total.length + 0.5).toLong)
    val l = pane.isListeningP
    if (l) model.removeListener(timelineListener)
    val newVisi = Span(pos, pos + visi.length)
    // println(s"updateFromScroll : $newVisi")
    model.visible = newVisi
    updateAxis()
    repaint()
    if (l) model.addListener(timelineListener)
  }

  private lazy val timePane = new BoxPanel(Orientation.Horizontal) {
    // contents += HStrut(meterPane.preferredSize.width)
    contents += timeAxis
  }
  private val scrollPane = new BoxPanel(Orientation.Horizontal) {
    contents += scroll
    contents += HStrut(16)
    listenTo(this)
  }

  protected def componentShown() {
    timelineModel.addListener(timelineListener)
    updateAxis()
    updateScroll()  // this adds scrollListener in the end
  }
  protected def componentHidden() {
    timelineModel.removeListener(timelineListener)
    scroll.reactions -= scrollListener
  }

  private object pane extends BorderPanel with DynamicComponentImpl {
    // println("INIT PANE")
    // protected val timelineModel = view.timelineModel

    def component = this

    def isListeningP = isListening

    // add(meterPane,  BorderPanel.Position.West  )
    add(timePane,         BorderPanel.Position.North )
    add(canvasComponent,  BorderPanel.Position.Center)
    add(scrollPane,       BorderPanel.Position.South )
    timelineModel.modifiableOption.foreach(TimelineNavigation.install(_, this))

    protected def componentShown () { view.componentShown () }
    protected def componentHidden() { view.componentHidden() }
  }

  final def component: Component = pane

  private val timelineListener: TimelineModel.Listener = {
    case TimelineModel.Visible(_, span) =>
      updateAxis()
      updateScroll()
      repaint()  // XXX TODO: optimize dirty region / copy double buffer

    case TimelineModel.Position(_, Change(before, now)) =>
      val mn = math.min(before, now)
      val mx = math.max(before, now)
      val x0 = math.max(0                            , frameToScreen(mn).toInt)
      val x1 = math.min(canvasComponent.peer.getWidth, frameToScreen(mx).toInt + 1)
      if (x0 < x1) {
        r.x      = x0
        r.width  = x1 - x0
        r.y      = 0
        r.height = timeAxis.peer.getHeight
        timeAxis.repaint(r)

        r.height = canvasComponent.peer.getHeight
        canvasComponent.repaint(r)
      }

    case TimelineModel.Selection(_, span) =>
      // XXX TODO: optimize dirty region
      timeAxis.repaint()
      repaint()
  }

  private val scrollListener: Reactions.Reaction = {
    case UIElementResized(_) =>
      updateScroll()

    case ValueChanged(_) =>
      // println(s"ScrollBar Value ${scroll.value}")
      timelineModel.modifiableOption.foreach(updateFromScroll)
  }

  @inline final protected def repaint() {
    canvasComponent.repaint()
  }

  private def processAxisMouse(model: TimelineModel.Modifiable, frame: Long) {
    axisMouseAction match {
      case AxisPosition =>
        model.position = frame
      case AxisSelection(fix) =>
        val span = Span(math.min(frame, fix), math.max(frame, fix))
        model.selection = if (span.isEmpty) Span.Void else span
    }
  }
}