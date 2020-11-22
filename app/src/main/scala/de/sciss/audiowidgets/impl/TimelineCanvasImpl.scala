/*
 *  TimelineCanvasImpl.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2020 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets
package impl

import de.sciss.desktop.impl.DynamicComponentImpl
import de.sciss.model.Change
import de.sciss.model.impl.ModelImpl
import de.sciss.span.Span

import java.awt.event.{AdjustmentEvent, AdjustmentListener}
import java.awt.image.BufferedImage
import java.awt.{Color, Graphics2D, Rectangle, TexturePaint}
import scala.collection.mutable
import scala.math.{max, min}
import scala.swing.Swing._
import scala.swing.event.{Key, MouseDragged, MousePressed, UIElementResized}
import scala.swing.{BorderPanel, BoxPanel, Component, Orientation, ScrollBar}

object TimelineCanvasImpl {
  private sealed trait      AxisMouseAction
  private case object       AxisPosition              extends AxisMouseAction
  private final case class  AxisSelection(fix: Long)  extends AxisMouseAction

  private final val colrPositionXor   = Color.black // new Color(0x00, 0x00, 0xFF, 0x7F)
  private final val colrPosition      = Color.white // new Color(0x00, 0x00, 0xFF, 0x7F)

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

  private[this] final var axisMouseAction: AxisMouseAction = AxisPosition

  // this is an auxiliary object which may be used
  // by any method on the EDT
  private[this] final val r = new Rectangle

  private[this] final val colrSelection         = Util.colrSelection
  private[this] final var catchBypassWasSynced  = false
  private[this] final val catchBypass           = mutable.Set.empty[Any]
  private[this] final var _catchEnabled         = true // false

  object transportCatch extends TransportCatch with ModelImpl[Boolean] {
    def catchEnabled: Boolean = _catchEnabled

    def catchEnabled_=(value: Boolean): Unit =
      if (_catchEnabled != value) {
        _catchEnabled       = value
        scrollAdjustBypass  = false
        ensureCatch()
        dispatch(value)
      }

    def addCatchBypass(token: Any): Unit = {
      //    requireEDT()
      val wasEmpty = catchBypass.isEmpty
      catchBypass += token
      if (wasEmpty) {
        val m = timelineModel
        catchBypassWasSynced = m.visible.contains(m.position)
      }
    }

    def removeCatchBypass (token: Any): Unit = {
      //    requireEDT()
      val becameEmpty = catchBypass.remove(token) && catchBypass.isEmpty
      if (becameEmpty && catchBypassWasSynced) {
        catchBypassWasSynced = false
        ensureCatch()
      }
    }
  }

  protected def transportRunning: Boolean

  private def ensureCatch(): Unit = if (_catchEnabled && catchBypass.isEmpty) {
    val m       = timelineModel
    val pos     = m.position
    val vis     = m.visible
    val visLen  = vis.length
    val tr      = transportRunning
    val posC    = if (tr) pos + visLen/6 else pos
    if (!vis.contains(posC)) {
      val start0  = if (tr || posC > vis.start) pos - visLen/6 else pos - visLen/2
      val total   = m.virtual
      val stop    = min(total.stop, max(total.start, start0) + visLen)
      val start   = max(total.start, stop - visLen)
      if (stop > start) {
        m.modifiableOption.foreach { mm =>
          mm.visible = Span(start, stop)
        }
      }
    }
  }

  protected def paintPosAndSelection(g: Graphics2D, h: Int): Unit = {
    val pos = frameToScreen(timelineModel.position).toInt
    g.getClipBounds(r)
    val rr  = r.x + r.width
    timelineModel.selection match {
      case Span(start, stop) =>
        val selX1 = frameToScreen(start).toInt
        val selX2 = frameToScreen(stop ).toInt
        if (selX1 < rr && selX2 > r.x) {
          g.setColor(colrSelection)
          g.fillRect(selX1, 0, selX2 - selX1, h)
          if (r.x  <= selX1)                g.drawLine(selX1    , 0, selX1    , h)
          if (selX2 > selX1 && rr >= selX2) g.drawLine(selX2 - 1, 0, selX2 - 1, h)
        }
      case _ =>
    }
    if (r.x <= pos && rr > pos) {
      g.setColor  (colrPosition)
      g.setXORMode(colrPositionXor)
      g.drawLine(pos, 0, pos, h)
      g.setPaintMode()
    }
  }

  protected def hours: Boolean = !timelineModel.clipStop ||
    timelineModel.bounds.stopOption.forall(stop => (stop / timelineModel.sampleRate) >= 3600.0)

  // lazy because of `timelineModel`
  private[this] final lazy val timeAxis: Axis = new Axis {
    override protected def paintComponent(g: Graphics2D): Unit = {
      super.paintComponent(g)
      paintPosAndSelection(g, peer.getHeight)
    }
    format = AxisFormat.Time(hours = hours, millis = true)

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
            val other = m.selection match {
              case Span.Void          => m.position
              case Span(start, stop)  => if (math.abs(frame - start) > math.abs(frame - stop)) start else stop
            }
            axisMouseAction = AxisSelection(other)
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

  private def updateAxis(): Unit = {
    val vis           = timelineModel.visible
    val sr            = timelineModel.sampleRate
    timeAxis.minimum  = vis.start / sr
    timeAxis.maximum  = vis.stop  / sr
  }

  private[this] final val scroll = new ScrollBar {
    orientation   = Orientation.Horizontal
    unitIncrement = 4
    reactions += {
      case UIElementResized(_) =>
        updateFromModel()
    }
  }

  private[this] var scrollValueSync = -1

  final def framesToScreen(numFrames: Long): Double = {
    val vis = timelineModel.visible
    numFrames.toDouble / vis.length * canvasComponent.peer.getWidth
  }

  final def frameToScreen(frame: Long): Double = {
    val vis = timelineModel.visible
    framesToScreen(frame - vis.start)
  }

  final def screenToFrame(screen: Int): Double = {
    val vis = timelineModel.visible
    screenToFrames(screen) + vis.start
  }

  final def screenToFrames(screen: Int): Double = {
    val vis = timelineModel.visible
    screen.toDouble / canvasComponent.peer.getWidth * vis.length
  }

  final def clipVisible(frame: Double): Long = {
    val vis = timelineModel.visible
    vis.clip(frame.toLong)
  }

  private def updateFromModel(): Unit = {
    val trackWidth      = max(1, scroll.peer.getWidth - 32)  // TODO XXX stupid hard coded value. but how to read it?
    val vis             = timelineModel.visible

    // __DO NOT USE deafTo and listenTo__ there must be a bug in scala-swing,
    // because that quickly overloads the AWT event multi-caster with stack overflows.
    //    deafTo(scroll)
    val ls = pane.isListeningP
    if (ls) scroll.peer.removeAdjustmentListener(scrollListener)

    val total           = timelineModel.virtual
    val framesPerPixel  = max(1, ((total.length + (trackWidth >> 1)) / trackWidth).toInt)
    val _max            = min(0x3FFFFFFFL, total.length / framesPerPixel).toInt
    val value           = min(_max - 1, (vis.start - total.start) / framesPerPixel).toInt
    val visAmt          = min(_max - value, vis.length / framesPerPixel).toInt
    val blockInc        = max(1, visAmt * 4 / 5)

    /*

      val framesPerPixel  = max(1, ((total.length + (trackWidth >> 1)) / trackWidth).toInt)
      value = (vis.start - total.start) / framesPerPixel
      value * framesPerPixel = (vis.start - total.start)
      vis.start = value * framesPerPixel + total.start

     */

    scroll.maximum        = _max
    scroll.visibleAmount  = visAmt
    scroll.value          = value
    scroll.blockIncrement = blockInc
    scrollValueSync       = value

    if (ls) scroll.peer.addAdjustmentListener(scrollListener)
  }

  private[this] var scrollWasAdjusting = false
  private[this] var scrollAdjustBypass = false

  private def updateFromScroll(model: TimelineModel.Modifiable, e: AdjustmentEvent): Unit = {
    val value           = scroll.value
    val vis             = model.visible
    val newVis          = if (scrollValueSync == value) vis else {
      val total           = model.virtual
      val trackWidth      = max(1, scroll.peer.getWidth - 32)  // TODO XXX stupid hard coded value. but how to read it?
      val framesPerPixel  = max(1, ((total.length + (trackWidth >> 1)) / trackWidth).toInt)
      // inverse of `updateFromModel`, to make sure there are no visual jumps
      // when touching the scroll bar
      val newVisStart     = min(total.stop - vis.length, value.toLong * framesPerPixel + total.start)
      Span(newVisStart, newVisStart + vis.length)
    }

    val isAdjusting = e.getValueIsAdjusting // scroll.valueIsAdjusting
    if (_catchEnabled && isAdjusting && !scrollWasAdjusting) {
      scrollAdjustBypass = true
//      println("addCatchBypass")
      transportCatch.addCatchBypass(scroll)

    } else if (scrollWasAdjusting && !isAdjusting && scrollAdjustBypass) {
      if (_catchEnabled && !newVis.contains(model.position)) { // we need to set prefCatch here even though laterInvocation will handle it,
        // because removeCatchBypass might look at it!
        transportCatch.catchEnabled = false
      }
      scrollAdjustBypass = false
//      println("removeCatchBypass")
      transportCatch.removeCatchBypass(scroll)
    }

    scrollValueSync     = value
    scrollWasAdjusting  = isAdjusting

    // old formula:
//    val pos   = min(total.stop - vis.length,
//      ((value.toDouble / scroll.maximum) * total.length + 0.5).toLong + total.start)

    if (newVis != vis) {
      val ls = pane.isListeningP
      if (ls) model.removeListener(timelineListener)
      model.visible = newVis
      updateAxis()
      repaint()
      if (ls) model.addListener(timelineListener)
    }
  }

  private[this] final lazy val timePane = new BoxPanel(Orientation.Horizontal) {
    contents += timeAxis
  }
  private[this] final val scrollPane = new BoxPanel(Orientation.Horizontal) {
    contents += scroll
    contents += HStrut(16)
  }

  protected def componentShown(): Unit = {
    timelineModel.addListener(timelineListener)
    updateAxis()
    updateFromModel()  // this adds scrollListener in the end
  }
  protected def componentHidden(): Unit = {
    timelineModel.removeListener(timelineListener)
    scroll.peer.removeAdjustmentListener(scrollListener)
  }

  private[this] object pane extends BorderPanel with DynamicComponentImpl {

    def isListeningP: Boolean = isListening

    add(timePane,         BorderPanel.Position.North )
    add(canvasComponent,  BorderPanel.Position.Center)
    add(scrollPane,       BorderPanel.Position.South )
    timelineModel.modifiableOption.foreach(TimelineNavigation.install(_, this))

    protected def componentShown (): Unit = view.componentShown ()
    protected def componentHidden(): Unit = view.componentHidden()
  }

  final def component: Component = pane

  private[this] val timelineListener: TimelineModel.Listener = {
    case _: TimelineModel.Visible | _: TimelineModel.Virtual =>
      updateAxis()
      updateFromModel()
      repaint()  // XXX TODO: optimize dirty region / copy double buffer

    case TimelineModel.Position(_, Change(before, now)) =>
      ensureCatch()
      val mn = min(before, now)
      val mx = max(before, now)
      val x0 = max(0                            , frameToScreen(mn).toInt - 1) // the `-1` is needed for shitty macOS retina display
      val x1 = min(canvasComponent.peer.getWidth, frameToScreen(mx).toInt + 1)
      if (x0 < x1) {
        r.x      = x0
        r.width  = x1 - x0
        r.y      = 0
        r.height = timeAxis.peer.getHeight
        timeAxis.repaint(r)

        r.height = canvasComponent.peer.getHeight
        canvasComponent.repaint(r)
      }

    case TimelineModel.Selection(_, _ /* span */) =>
      // XXX TODO: optimize dirty region
      timeAxis.repaint()
      repaint()
  }

  // N.B.: We used `reactions` in scala-swing, but there seems to
  // be a problem with temporarily removing them during dispatch,
  // see https://github.com/scala/scala-swing/issues/141
  private[this] val scrollListener = new AdjustmentListener {
    def adjustmentValueChanged(e: AdjustmentEvent): Unit =
      timelineModel.modifiableOption.foreach(updateFromScroll(_, e))
  }

  @inline final protected def repaint(): Unit = canvasComponent.repaint()

  private def processAxisMouse(model: TimelineModel.Modifiable, frame: Long): Unit =
    axisMouseAction match {
      case AxisPosition =>
        model.position = frame
      case AxisSelection(fix) =>
        val span = Span(min(frame, fix), max(frame, fix))
        model.selection = if (span.isEmpty) Span.Void else span
    }
}