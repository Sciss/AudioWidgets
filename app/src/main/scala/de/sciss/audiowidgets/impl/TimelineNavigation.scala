/*
 *  TimelineNavigation.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2018 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets
package impl

import de.sciss.desktop
import de.sciss.desktop.{FocusType, KeyStrokes}
import de.sciss.span.Span
import javax.swing.KeyStroke

import scala.math.{max, min}
import scala.swing.event.Key
import scala.swing.{Action, Component}

object TimelineNavigation {
  /** Installs standard keyboard commands for navigating a timeline model.
    * These commands are enabled when the `component` is inside the focused window.
    */
  def install(model: TimelineModel.Modifiable, component: Component): Unit = {
    import FocusType.{Window => Focus}
    import KeyStrokes._
    import desktop.Implicits._

    // ---- zoom ----
    component.addAction("timeline-inc-h1"  , new ActionSpanWidth(model, 2.0, ctrl  + Key.Left        ), Focus)
    component.addAction("timeline-inc-h2"  , new ActionSpanWidth(model, 2.0, menu1 + Key.OpenBracket ), Focus)
    component.addAction("timeline-dec-h1"  , new ActionSpanWidth(model, 0.5, ctrl  + Key.Right       ), Focus)
    component.addAction("timeline-dec-h2"  , new ActionSpanWidth(model, 0.5, menu1 + Key.CloseBracket), Focus)

    component.addAction("timeline-pos-sel1", new ActionSelToPos(model, 0.0, deselect = true,  plain + Key.Up  ), Focus)
    component.addAction("timeline-pos-sel2", new ActionSelToPos(model, 1.0, deselect = true,  plain + Key.Down), Focus)
    component.addAction("timeline-pos-sel3", new ActionSelToPos(model, 0.0, deselect = false, alt   + Key.Up  ), Focus)
    component.addAction("timeline-pos-sel4", new ActionSelToPos(model, 1.0, deselect = false, alt   + Key.Down), Focus)

    import ActionScroll._
    component.addAction("timeline-return"  , new ActionScroll(model, BoundsStart   , plain + Key.Enter), Focus)
    component.addAction("timeline-left"    , new ActionScroll(model, SelectionStart, plain + Key.Left ), Focus)
    component.addAction("timeline-right"   , new ActionScroll(model, SelectionStop , plain + Key.Right), Focus)
    component.addAction("timeline-fit"     , new ActionScroll(model, FitToSelection, alt   + Key.F    ), Focus)
    component.addAction("timeline-entire1" , new ActionScroll(model, EntireBounds  , alt   + Key.A    ), Focus)
    component.addAction("timeline-entire2" , new ActionScroll(model, EntireBounds  , menu2 + Key.Left ), Focus)

    import ActionSelect._
    component.addAction("timeline-sel-to-beg", new ActionSelect(model, ExtendToBoundsStart, shift + Key.Enter), Focus)
    component.addAction("timeline-sel-to-end", new ActionSelect(model, ExtendToBoundsStop , shift + alt + Key.Enter), Focus)
    component.addAction("timeline-sel-all"   , new ActionSelect(model, All, menu1 + Key.A), FocusType.Default)

    component.addAction("timeline-go-to-time", new ActionGoToTime(model, stroke = plain + Key.G), Focus)
  }

  private def minStart(model: TimelineModel): Long = 
    if (model.clipStart) model.bounds.startOrElse (-0x2000000000000000L) else -0x2000000000000000L
  
  private def maxStop (model: TimelineModel): Long = 
    if (model.clipStop ) model.bounds.stopOrElse  (+0x2000000000000000L) else +0x2000000000000000L

  final protected class ActionSpanWidth(model: TimelineModel.Modifiable, factor: Double, stroke: KeyStroke)
    extends Action(s"Span Width $factor") {

    accelerator = Some(stroke)

    def apply(): Unit = {
      val visSpan     = model.visible
      val visLen      = visSpan.length
      val pos         = model.position

      val newVisSpan = if (factor < 1.0) {
        // zoom in
        if (visLen < 4) Span.Void
        else {
          // if timeline pos visible -> try to keep it's relative position constant
          if (visSpan.contains(pos)) {
            val start = pos - ((pos - visSpan.start) * factor + 0.5).toLong
            val stop = start + (visLen * factor + 0.5).toLong
            Span(start, stop)
            // if timeline pos before visible span, zoom left hand
          } else if (visSpan.start > pos) {
            val start = visSpan.start
            val stop = start + (visLen * factor + 0.5).toLong
            Span(start, stop)
            // if timeline pos after visible span, zoom right hand
          } else {
            val stop = visSpan.stop
            val start = stop - (visLen * factor + 0.5).toLong
            Span(start, stop)
          }
        }
      } else {
        // zoom out
        val start0    = visSpan.start - (visLen * factor / 4 + 0.5).toLong
        val start     = max(minStart(model), start0)
        val stop0     = start + (visLen * factor + 0.5).toLong
        val stop      = min(maxStop(model),  stop0)
        Span(start, stop)
      }
      newVisSpan match {
        case sp @ Span(_, _) if sp.nonEmpty => model.visible = sp
        case _ =>
      }
    }
  }

  final protected class ActionSelToPos(model: TimelineModel.Modifiable, weight: Double, deselect: Boolean,
                                       stroke: KeyStroke)
    extends Action("Extends Selection to Position") {

    accelerator = Some(stroke)

    def apply(): Unit = {
      model.selection match {
        case sel @ Span(selStart, _) =>
          if (deselect) model.selection = Span.Void
          val pos = (selStart + sel.length * weight + 0.5).toLong
          model.position = pos

        case _ =>
      }
    }
  }

  protected object ActionScroll {
    sealed trait Mode
    case object BoundsStart     extends Mode
    sealed trait NotBoundsStart extends Mode
    case object SelectionStart  extends NotBoundsStart
    case object SelectionStop   extends NotBoundsStart
    case object FitToSelection  extends NotBoundsStart
    case object EntireBounds    extends NotBoundsStart
   }

  final protected class ActionScroll(model: TimelineModel.Modifiable, mode: ActionScroll.Mode, stroke: KeyStroke)
    extends Action("Scroll") {

    accelerator = Some(stroke)

    import ActionScroll._

    def apply(): Unit = {
      val pos       = model.position
      val visSpan   = model.visible
      val wholeSpan = model.bounds

      mode match {
        case BoundsStart =>
          wholeSpan.startOption.foreach { start =>
            val posNotZero = pos != start
            val zeroNotVis = !visSpan.contains(start)
            if (posNotZero || zeroNotVis) {
              if (posNotZero) model.position = start
              if (zeroNotVis) {
                model.visible = Span(start, start + visSpan.length)
              }
            }
          }

        case mode2: NotBoundsStart =>
          val selSpan = model.selection
          val newSpan = mode2 match {
            case SelectionStart =>
              val selSpanStart = selSpan match {
                case Span.HasStart(s) => s
                case _                => pos
              }
              val start0    = selSpanStart - (visSpan.length >> (if (visSpan.contains(selSpanStart)) 1 else 3))
              val start     = max(minStart(model), start0)
              val stop      = min(maxStop (model), start + visSpan.length)
              Span(start, stop)

            case SelectionStop =>
              val selSpanStop = selSpan match {
                case Span.HasStop(s)  => s
                case _                => pos
              }
              val stop0 = selSpanStop + (visSpan.length >> (if (visSpan.contains(selSpanStop)) 1 else 3))
              val stop  = min(maxStop(model), stop0)
              val start = max(minStart(model), stop - visSpan.length)
              Span(start, stop)

            case FitToSelection => selSpan
            case EntireBounds   => wholeSpan
          }
          newSpan match {
            case sp: Span if sp.nonEmpty && sp != visSpan =>
              model.visible = sp
            case _ =>
          }
      }
    }
  }

  protected object ActionSelect {
    sealed trait Mode
    case object ExtendToBoundsStart extends Mode
    case object ExtendToBoundsStop  extends Mode
    case object All                 extends Mode
    case object FlipBackward        extends Mode
    case object FlipForward         extends Mode
   }

 final protected class ActionSelect(model: TimelineModel.Modifiable, mode: ActionSelect.Mode, stroke: KeyStroke = null)
   extends Action("Select") {

   accelerator = Option(stroke)

   import ActionSelect._

   def apply(): Unit = {
     val pos      = model.position
     val selSpan  = model.selection match {
       case sp: Span => sp
       case _        => Span(pos, pos)
     }

     val wholeSpan  = model.bounds
     val newSpan    = mode match {
       case ExtendToBoundsStart =>
         wholeSpan.startOption.fold(selSpan)(start => Span(start, selSpan.stop))
       
       case ExtendToBoundsStop => 
         wholeSpan.stopOption.fold(selSpan)(stop => Span(selSpan.start, stop))
       
       case All => wholeSpan match {
         case sp: Span  => sp
         case _         => selSpan
       }
         
       case FlipBackward =>
         val delta0 = -selSpan.length
         val delta  = if (!model.clipStart) delta0 else 
           wholeSpan.startOption.fold(delta0)(start => max(start - selSpan.start, delta0))
         selSpan.shift(delta)
       
       case FlipForward =>
         val delta0 = selSpan.length
         val delta  = if (!model.clipStop) delta0 else
           wholeSpan.stopOption.fold(delta0)(stop => min(stop - selSpan.stop, delta0))
         selSpan.shift(delta)
     }
     if (newSpan != selSpan) model.selection = newSpan
   }
 }
}