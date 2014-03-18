/*
 *  DualRangeSlider.java
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

import scala.swing.Component
import javax.swing.event.{ChangeEvent, ChangeListener}
import scala.swing.event.ValueChanged

class DualRangeSlider(model0: DualRangeModel) extends Component with DualRangeSliderLike {
  me =>

  override lazy val peer: j.DualRangeSlider = new j.DualRangeSlider(model0) with SuperMixin

  def model        : DualRangeModel         = peer.model
  def model_=(value: DualRangeModel): Unit  = peer.model = value

  def valueEditable        : Boolean        = peer.valueEditable
  def valueEditable_=(value: Boolean): Unit = peer.valueEditable = value

  def rangeEditable        : Boolean        = peer.rangeEditable
  def rangeEditable_=(value: Boolean): Unit = peer.rangeEditable = value

  def valueVisible         : Boolean        = peer.valueVisible
  def valueVisible_= (value: Boolean): Unit = peer.valueVisible  = value

  def rangeVisible         : Boolean        = peer.rangeVisible
  def rangeVisible_= (value: Boolean): Unit = peer.rangeVisible  = value

  def extentFixed          : Boolean        = peer.extentFixed
  def extentFixed_=  (value: Boolean): Unit = peer.extentFixed   = value

  peer.addChangeListener(new ChangeListener {
    def stateChanged(e: ChangeEvent): Unit = publish(new ValueChanged(me))
  })
}
