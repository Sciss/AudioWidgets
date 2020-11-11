/*
 *  ActionGoToTime.scala
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

import java.awt.geom.Path2D

import de.sciss.desktop
import de.sciss.desktop.{FocusType, KeyStrokes, OptionPane}
import de.sciss.icons.raphael
import de.sciss.swingplus.DoClickAction
import javax.swing.KeyStroke

import scala.swing.event.Key
import scala.swing.{Action, BoxPanel, Button, Component, FlowPanel, Orientation, Panel, Swing}

class ActionGoToTime(model: TimelineModel.Modifiable, stroke: KeyStroke)
  extends Action("Set Cursor Position") {

  accelerator = Option(stroke)

  import model.{bounds => bounds}

  private def mkBut(shape: Path2D => Unit, key: KeyStroke, fun: => Option[Long]): Button = {
    val action = new Action(null) {
      icon = raphael.TexturedIcon(20)(shape)

      def apply(): Unit = fun.foreach { value =>
        ggTime.value = value
        ggFocus.requestFocus()
      }
    }
    action.enabled = fun.isDefined
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
    val ggStart   = mkBut(raphael.Shapes.TransportBegin, menu1 + Key.Comma , bounds.startOption)
    val ggCurrent = mkBut(raphael.Shapes.Location      , menu1 + Key.Period, Some(model.position))
    val ggEnd     = mkBut(raphael.Shapes.TransportEnd  , menu1 + Key.Slash , bounds.stopOption)
    val res       = new FlowPanel(ggStart, ggCurrent, ggEnd)
    res.hGap = 0
    res.vGap = 0
    res
  }

  private[this] lazy val ggTime: ParamField[Long] =
    new TimeField(value0 = bounds.startOrElse(bounds.clip(0L)), span0 = bounds,
      sampleRate = model.sampleRate, viewSampleRate0 = 0.0,
      clipStart = model.clipStart, clipStop = model.clipStop)

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
