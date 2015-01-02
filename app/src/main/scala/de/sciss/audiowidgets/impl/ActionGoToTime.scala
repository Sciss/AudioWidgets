/*
 *  ActionGoToTime.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2015 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets
package impl

import java.awt.Color
import java.awt.geom.Path2D
import java.text.{NumberFormat, ParseException}
import java.util.Locale
import javax.swing.KeyStroke
import javax.swing.text.{NumberFormatter, MaskFormatter}

import de.sciss.desktop
import de.sciss.desktop.{KeyStrokes, FocusType, OptionPane}
import de.sciss.icons.raphael
import de.sciss.swingplus.DoClickAction

import scala.swing.event.Key
import scala.swing.{Swing, Orientation, BoxPanel, Panel, Button, Component, FlowPanel, Action}
import scala.util.Try

class ActionGoToTime(model: TimelineModel.Modifiable, stroke: KeyStroke)
  extends Action("Set Cursor Position") {

  accelerator = Option(stroke)

  import model.bounds

  private def framesToMillis (n: Long): Double =  n / model.sampleRate * 1000
  private def millisToFrames (m: Double): Long = (m * model.sampleRate / 1000 + 0.5).toLong
  private def framesToPercent(n: Long): Double = (n - bounds.start).toDouble / bounds.length
  private def percentToFrames(p: Double): Long = (p * bounds.length + bounds.start + 0.5).toLong

  private def mkBut(shape: Path2D => Unit, key: KeyStroke, fun: => Long): Button = {
    val action = new Action(null) {
      icon = raphael.Icon(extent = 20, fill = raphael.TexturePaint(24), shadow = raphael.WhiteShadow)(shape)

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

  private lazy val butPane: Panel = {
    import KeyStrokes.menu1
    // meta-left and meta-right would have been better, but
    // somehow the textfield blocks these inputs
    val ggStart   = mkBut(raphael.Shapes.TransportBegin, menu1 + Key.Comma , bounds.start  )
    val ggCurrent = mkBut(raphael.Shapes.Location      , menu1 + Key.Period, model.position)
    val ggEnd     = mkBut(raphael.Shapes.TransportEnd  , menu1 + Key.Slash , bounds.stop   )
    val res       = new FlowPanel(ggStart, ggCurrent, ggEnd)
    res.hGap = 0
    res.vGap = 0
    res
  }

  private lazy val ggTime: ParamField[Long] = {
    val fmtTime = new ParamFormat[Long] {
      private val axis = AxisFormat.Time(hours = true, millis = true)

      val unit = UnitView("HH:MM:SS.mmm", raphael.Icon(20, Color.darkGray)(raphael.Shapes.WallClock))

      val formatter = new MaskFormatter("*#:##:##.###") {
        // setAllowsInvalid(true)
        setPlaceholderCharacter('_')
        // the next line is crucial because retarded MaskFormatter calls into super.stringToValue
        // from other methods, and DefaultFormatter finds a Long with a static constructor method
        // that takes a String, and blindly tries to feed that crap into it. Setting an explicit
        // value-class that does _not_ have a string constructor will bypass that bull crap.
        setValueClass(classOf[AnyRef])

        override def stringToValue(value: String): AnyRef = {
          val v0 = super.stringToValue(value)
          val s0 = v0.toString
          val res = tryParse(s0)
          // println(s"res = $res")
          res.asInstanceOf[AnyRef]
        }

        override def valueToString(value: Any): String = {
          val s0 = format(value.asInstanceOf[Long])
          super.valueToString(s0)
        }
      }

      def adjust(in: Long, inc: Int): Long = {
        val incM = millisToFrames(inc)
        bounds.clip(in + incM)
      }

      private def tryParse(s: String): Long = {
        // HH:MM:SS.mmm
        val arr = s.replace(' ', '0').split(':')
        if (arr.size != 3) throw new ParseException(s, 0)
        try {
          val hours   = arr(0).toLong
          val minutes = arr(1).toLong
          val secs    = arr(2).toDouble
          val millis  = (secs * 1000).toLong
          val allMS   = (hours * 60 + minutes) * 60000 + millis
          bounds.clip(millisToFrames(allMS))
        } catch {
          case _: NumberFormatException => throw new ParseException(s, 0)
        }
      }

      def parse(s: String): Option[Long] = Try(tryParse(s)).toOption

      def format(value: Long): String = axis.format(value / model.sampleRate, pad = 12)
    }

    val fmtFrames = new ParamFormat[Long] {
      val unit = UnitView("sample frames", raphael.Icon(20, Color.darkGray)(Shapes.SampleFrames))

      private val numFmt = NumberFormat.getIntegerInstance(Locale.US)
      numFmt.setGroupingUsed(false)
      val formatter = new NumberFormatter(numFmt)
      formatter.setMinimum(bounds.start)
      formatter.setMaximum(bounds.stop )

      def adjust(in: Long, inc: Int): Long =
        model.bounds.clip(in + inc)

      def parse(s: String): Option[Long] = Try(s.toLong).toOption

      def format(value: Long): String = value.toString
    }

    val fmtMilli = new ParamFormat[Long] {
      val unit = UnitView("milliseconds", "ms")

      private val numFmt = NumberFormat.getIntegerInstance(Locale.US)
      numFmt.setGroupingUsed(false)
      val formatter = new NumberFormatter(numFmt) {
        override def valueToString(value: Any): String = format(value.asInstanceOf[Long])

        override def stringToValue(text: String): AnyRef = tryParse(text).asInstanceOf[AnyRef]
      }
      formatter.setMinimum(framesToMillis(bounds.start))
      formatter.setMaximum(framesToMillis(bounds.stop ))

      def adjust(in: Long, inc: Int): Long = {
        val incM = millisToFrames(inc)
        model.bounds.clip(in + incM)
      }

      private def tryParse(s: String): Long =
      try {
        bounds.clip(millisToFrames(s.toLong))
      } catch {
        case _: NumberFormatException => throw new ParseException(s, 0)
      }

      def parse(s: String): Option[Long] = Try(tryParse(s)).toOption

      def format(value: Long): String = (framesToMillis(value) + 0.5).toLong.toString
    }

    val fmtPercent = new ParamFormat[Long] {
      val unit = UnitView("percent", "%")

      private val numFmt = NumberFormat.getIntegerInstance(Locale.US)
      numFmt.setGroupingUsed(false)
      val formatter = new NumberFormatter(numFmt) {
        override def valueToString(value: Any   ): String = format(value.asInstanceOf[Long])
        override def stringToValue(text : String): AnyRef = tryParse(text).asInstanceOf[AnyRef]
      }
      formatter.setMinimum(  0.0)
      formatter.setMaximum(100.0)

      private def tryParse(s: String): Long =
        try {
          bounds.clip(percentToFrames(s.toDouble * 0.01))
        } catch {
          case _: NumberFormatException => throw new ParseException(s, 0)
        }

      def adjust(in: Long, inc: Int): Long = {
        val n = percentToFrames(framesToPercent(in) + inc * 0.0001)
        model.bounds.clip(n)
      }

      def parse(s: String): Option[Long] = Try(tryParse(s)).toOption

      def format(value: Long): String = f"${framesToPercent(value) * 100}%1.3f"
    }

    val fmt = List(fmtTime, fmtFrames, fmtMilli, fmtPercent)
    val res = new ParamField(bounds.start /* model.position */, fmt)
    //    val iMap = res.getInputMap
    //    iMap.remove(KeyStrokes.menu1 + Key.Left )
    //    iMap.remove(KeyStrokes.menu1 + Key.Right)

    res.prototypeDisplayValues = bounds.start :: bounds.stop :: Nil
    res
  }

  private def ggFocus: Component = ggTime.textField

  private lazy val pane = new BoxPanel(Orientation.Vertical) {
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
