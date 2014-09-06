/*
 *  Transport.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2014 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets

import j.TransportCompanion
import swing.{Button, Orientation, BoxPanel, AbstractButton, Component}
import collection.immutable.{IndexedSeq => Vec}

object Transport extends TransportCompanion {
  type ComponentType      = Component
  type AbstractButtonType = AbstractButton
  type Action             = swing.Action with ActionLike

  protected def makeAction(icn: IconImpl, fun: => Unit): Action = new ActionImpl(icn, fun)

  private final class SButtonStripImpl(protected val actions: Seq[Action], protected val scheme: ColorScheme)
    extends BoxPanel(Orientation.Horizontal) with ButtonStripImpl {

    protected def addButtons(seq: Vec[AbstractButton]): Unit = seq.foreach(contents += _)

    protected def makeButton(pos: String, action: Action): AbstractButton = {
      val b = new Button(action)
      b.focusable = false
      b.peer.putClientProperty("JButton.buttonType", "segmentedCapsule") // "segmented" "segmentedRoundRect" "segmentedCapsule" "segmentedTextured" "segmentedGradient"
      b.peer.putClientProperty("JButton.segmentPosition", pos)
      b
    }
  }

  def makeButtonStrip(actions: Seq[ActionElement], scale: Float, scheme: ColorScheme = DarkScheme): Component with ButtonStrip = {
    val a = actions.map(_.apply(scale, scheme))
    new SButtonStripImpl(a, scheme)
  }

  private final class ActionImpl(icn: IconImpl, fun: => Unit) extends swing.Action(null) with ActionLike {
    icon = icn

    def element: Element = icn.element
    def scale  : Float   = icn.scale

    def apply(): Unit = fun
  }
}