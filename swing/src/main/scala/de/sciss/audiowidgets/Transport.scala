/*
 *  Transport.scala
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

import javax.swing.Icon

import de.sciss.audiowidgets.j.TransportCompanion

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.swing.{AbstractButton, BoxPanel, Button, Component, Orientation}

object Transport extends TransportCompanion {
  type ComponentType      = Component
  type AbstractButtonType = AbstractButton
  type Action             = swing.Action with ActionLike

  protected def makeAction(icons: (Icon, Icon, Icon, Icon), element: Element, scale: Float, fun: => Unit): Action =
    new ActionImpl(icons, element, scale, fun)

  private final class SButtonStripImpl(protected val actions: Seq[Action], protected val scheme: ColorScheme)
    extends BoxPanel(Orientation.Horizontal) with ButtonStripImpl {

    protected def addButtons(seq: Vec[AbstractButton]): Unit = seq.foreach(contents += _)

    protected def makeButton(pos: String, action: Action): AbstractButton = {
      val b = new Button(action)
      val (_, iconSelected, iconPressed, iconDisabled) = action.icons
      b.peer.setSelectedIcon(iconSelected)
      b.peer.setPressedIcon (iconPressed )
      b.peer.setDisabledIcon(iconDisabled)
      b.focusable = false
      b.peer.putClientProperty("styleId", "icon-space")
      b.peer.putClientProperty("JButton.buttonType", "segmentedCapsule") // "segmented" "segmentedRoundRect" "segmentedCapsule" "segmentedTextured" "segmentedGradient"
      b.peer.putClientProperty("JButton.segmentPosition", pos)
      b
    }
  }

  def makeButtonStrip(actions: Seq[ActionElement], scale: Float,
                      scheme: ColorScheme = defaultColorScheme): Component with ButtonStrip = {
    val a = actions.map(_.apply(scale, scheme))
    new SButtonStripImpl(a, scheme)
  }

  private final class ActionImpl(val icons: (Icon, Icon, Icon, Icon), val element: Element, val scale:
                                 Float, fun: => Unit)
    extends swing.Action(null) with ActionLike {

    icon = icons._1

    def apply(): Unit = fun
  }
}