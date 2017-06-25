/*
 *  ActionGoToTime.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2016 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets
package impl

import java.awt.geom.Path2D
import javax.swing.KeyStroke

import de.sciss.desktop
import de.sciss.desktop.{FocusType, KeyStrokes, OptionPane}
import de.sciss.icons.raphael
import de.sciss.span.Span
import de.sciss.swingplus.DoClickAction

import scala.swing.event.Key
import scala.swing.{Action, BoxPanel, Button, Component, FlowPanel, Orientation, Panel, Swing}

class ActionGoToTime(model: TimelineModel.Modifiable, stroke: KeyStroke)
  extends Action("Set Cursor Position") {

  accelerator = Option(stroke)

  import model.bounds

  private def mkBut(shape: Path2D => Unit, key: KeyStroke, fun: => Long): Button = {
    val action = new Action(null) {
      icon = raphael.TexturedIcon(20)(shape)

      def apply(): Unit = {
        ggTime.value = fun
        ggFocus.requestFocus()
      }
    }
    val but = new Button(action)
    import desktop.Implicits._
    val clickCurr = DoClickAction(but)
    clickCurr.accelerator = Some(key)
    but.addAction("param-current", clickCurr, FocusType.Window)
    but
  }

  private[this] lazy val butPane: Panel = {
    import KeyStrokes.menu1
    // meta-left and meta-right would have been better, but
    // somehow the text-field blocks these inputs
    val ggStart   = mkBut(raphael.Shapes.TransportBegin, menu1 + Key.Comma , bounds.start  )
    val ggCurrent = mkBut(raphael.Shapes.Location      , menu1 + Key.Period, model.position)
    val ggEnd     = mkBut(raphael.Shapes.TransportEnd  , menu1 + Key.Slash , bounds.stop   )
    val res       = new FlowPanel(ggStart, ggCurrent, ggEnd)
    res.hGap = 0
    res.vGap = 0
    res
  }

  private[this] lazy val ggTime: ParamField[Long] =
    new TimeField(value0 = model.bounds match { case hs: Span.HasStart => hs.start; case _ => 0L },
      span0 = model.bounds, sampleRate = model.sampleRate)

  private def ggFocus: Component = ggTime.textField

  private[this] lazy val pane = new BoxPanel(Orientation.Vertical) {
    contents += butPane
    contents += Swing.VStrut(8)
    contents += ggTime
    contents += Swing.VStrut(12)
  }

  def apply(): Unit = {
    val opt = OptionPane.confirmation(message = pane, messageType = OptionPane.Message.Question,
      optionType = OptionPane.Options.OkCancel, focus = Some(ggFocus))
    val res = opt.show(None, title)  // XXX TODO - find window
    if (res == OptionPane.Result.Ok) {
      model.position = ggTime.value
    }
  }
}
