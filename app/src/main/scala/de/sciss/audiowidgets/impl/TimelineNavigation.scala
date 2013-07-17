package de.sciss.audiowidgets
package impl

import scala.swing.{Action, Component}
import de.sciss.desktop.{FocusType, KeyStrokes}
import de.sciss.desktop
import javax.swing.KeyStroke
import de.sciss.span.Span
import java.awt.event.KeyEvent

object TimelineNavigation {
  def install(model: TimelineModel.Modifiable, component: Component) {
    import KeyStrokes._
    import KeyEvent._
    import desktop.Implicits._

    component.addAction("timeline-inch1", new ActionSpanWidth(model, 2.0, ctrl  + VK_LEFT         ), FocusType.Window)
    component.addAction("timeline-inch2", new ActionSpanWidth(model, 2.0, menu1 + VK_OPEN_BRACKET ), FocusType.Window)
    component.addAction("timeline-dech1", new ActionSpanWidth(model, 0.5, ctrl  + VK_RIGHT        ), FocusType.Window)
    component.addAction("timeline-dech2", new ActionSpanWidth(model, 0.5, menu1 + VK_CLOSE_BRACKET), FocusType.Window)
  }

  //  import ActionScroll._
  //  addAction("retn",     new ActionScroll(SCROLL_SESSION_START,    stroke(VK_ENTER,  0       )))
  //  addAction("left",     new ActionScroll(SCROLL_SELECTION_START,  stroke(VK_LEFT,   0       )))
  //  addAction("right",    new ActionScroll(SCROLL_SELECTION_STOP,   stroke(VK_RIGHT,  0       )))
  //  addAction("fit",      new ActionScroll(SCROLL_FIT_TO_SELECTION, stroke(VK_F,      ALT_MASK)))
  //  addAction("entire1",  new ActionScroll(SCROLL_ENTIRE_SESSION,   stroke(VK_A,      ALT_MASK)))
  //  addAction("entire2",  new ActionScroll(SCROLL_ENTIRE_SESSION,   stroke(VK_LEFT,   meta2   )))
  //  import ActionSelect._
  //  addAction("seltobeg", new ActionSelect(SELECT_TO_SESSION_START, stroke(VK_ENTER, SHIFT_MASK           )))
  //  addAction("seltoend", new ActionSelect(SELECT_TO_SESSION_END,   stroke(VK_ENTER, SHIFT_MASK | ALT_MASK)))
  //
  //  addAction("posselbegc", new ActionSelToPos(0.0, deselect = true,  stroke(VK_UP,   0       )))
  //  addAction("posselendc", new ActionSelToPos(1.0, deselect = true,  stroke(VK_DOWN, 0       )))
  //  addAction("posselbeg",  new ActionSelToPos(0.0, deselect = false, stroke(VK_UP,   ALT_MASK)))
  //  addAction("posselend",  new ActionSelToPos(1.0, deselect = false, stroke(VK_DOWN, ALT_MASK)))

  private class ActionSpanWidth(model: TimelineModel.Modifiable, factor: Double, stroke: KeyStroke)
    extends Action(s"Span Width $factor") {

    accelerator = Some(stroke)

    def apply() {
      val visiSpan    = model.visible
      val visiLen     = visiSpan.length
      val pos         = model.position

      val newVisiSpan = if (factor < 1.0) {
        // zoom in
        if (visiLen < 4) Span.Void
        else {
          // if timeline pos visible -> try to keep it's relative position constant
          if (visiSpan.contains(pos)) {
            val start = pos - ((pos - visiSpan.start) * factor + 0.5).toLong
            val stop = start + (visiLen * factor + 0.5).toLong
            Span(start, stop)
            // if timeline pos before visible span, zoom left hand
          } else if (visiSpan.start > pos) {
            val start = visiSpan.start
            val stop = start + (visiLen * factor + 0.5).toLong
            Span(start, stop)
            // if timeline pos after visible span, zoom right hand
          } else {
            val stop = visiSpan.stop
            val start = stop - (visiLen * factor + 0.5).toLong
            Span(start, stop)
          }
        }
      } else {
        // zoom out
        val total = model.bounds
        val start = math.max(total.start, visiSpan.start - (visiLen * factor / 4 + 0.5).toLong)
        val stop  = math.min(total.stop,  start + (visiLen * factor + 0.5).toLong)
        Span(start, stop)
      }
      newVisiSpan match {
        case sp @ Span(_, _) if sp.nonEmpty => model.visible = sp
        case _ =>
      }
    }
  }

  // class actionSpanWidthClass
}