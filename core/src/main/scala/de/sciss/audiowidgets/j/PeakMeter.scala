/*
 *  PeakMeter.java
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
package j

import java.awt.{Color, Font, Graphics, Insets}
import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import javax.swing.{BorderFactory, BoxLayout, JPanel, SwingConstants}

import scala.annotation.switch
import scala.collection.immutable.{IndexedSeq => Vec}

object PeakMeter {
  final val DefaultHoldDuration = 2500
}

class PeakMeter(orient: Int = SwingConstants.VERTICAL) 
  extends JPanel with PeakMeterLike {

  import SwingConstants._

  import PeakMeter._

  protected var meters                      = new Array[PeakMeterBar](0)
  protected var captionComp: PeakMeterCaption = _

  private[this] final var holdDurationVar	        = DefaultHoldDuration   // milliseconds peak hold
	private[this] final var captionPositionVar	    = LEFT
	private[this] final var captionAlign	          = RIGHT
	private[this] final var captionVisibleVar	      = true
	private[this] final var captionLabelsVar	      = true
	private[this] final var numChannelsVar		      = 0
	private[this] final var borderVisibleVar			  = false

	private[this] final var rmsPaintedVar		        = true
	private[this] final var holdPaintedVar		      = true

	private[this] final var orientVar			          = VERTICAL
	private[this] final var vertical		            = true

  private[this] final val ins                     = new Insets(0, 0, 0, 0)
  private[this] final var ticksVar		            = 101

  setLayout(new BoxLayout(this, BoxLayout.X_AXIS))

  setFont(new Font("SansSerif", Font.PLAIN, 12))
  addPropertyChangeListener("font", new PropertyChangeListener {
    def propertyChange(e: PropertyChangeEvent): Unit = if (captionComp != null) {
      captionComp.setFont(getFont)
      val b = BorderFactory.createEmptyBorder(captionComp.ascent, 1, captionComp.descent, 1)
      var ch = 0
      while (ch < meters.length) {
        meters(ch).setBorder(b)
        ch += 1
      }
    }
  })
  updateBorders()

  def orientation_=(value: Int): Unit = if (orientVar != value) {
    if (value != HORIZONTAL && value != VERTICAL) throw new IllegalArgumentException(value.toString)
    orientVar = value
    vertical = orientVar == VERTICAL
    if (captionComp != null) captionComp.orientation = orientVar
    var ch = 0
    while (ch < meters.length) {
      meters(ch).orientation = orientVar
      ch += 1
    }
    setLayout(new BoxLayout(this, if (vertical) BoxLayout.X_AXIS else BoxLayout.Y_AXIS))
    updateBorders()
    revalidate()
  }

  def orientation: Int = orientVar

  def channel(ch: Int): PeakMeterChannel = meters(ch)

  def ticks_=(num: Int): Unit = if (ticksVar != num) {
    ticksVar = num
    if (captionComp != null) {
      captionComp.ticks = num
    }
    var ch = 0
    while (ch < meters.length) {
      meters(ch).ticks = num
      ch += 1
    }
  }

  def ticks: Int = ticksVar

  def rmsPainted_=(b: Boolean): Unit = if (rmsPaintedVar != b) {
    rmsPaintedVar = b
    var ch = 0
    while (ch < meters.length) {
      meters(ch).rmsPainted = b
      ch += 1
    }
  }

  def rmsPainted: Boolean = rmsPaintedVar

  def holdPainted_=(b: Boolean): Unit = if (holdPaintedVar != b) {
    holdPaintedVar = b
    var ch = 0
    while (ch < meters.length) {
      meters(ch).holdPainted = b
      ch += 1
    }
  }

  def holdPainted: Boolean = holdPaintedVar

  def update(values: Vec[Float], offset: Int, time: Long): Boolean = {
    val metersCopy  = meters // = easy synchronization
    val numMeters   = math.min(metersCopy.length, (values.length - offset) >> 1)
    var dirty = 0

    var ch  = 0
    var off = offset
    while (ch < numMeters) {
      val peak = values(off)
      off += 1
      val rms = values(off)
      off += 1
      if (metersCopy(ch).update(peak, rms, time)) dirty += 1
      ch += 1
    }

    dirty > 0
  }

  def holdDuration_=( millis: Int ): Unit = if (holdDurationVar != millis) {
    holdDurationVar = millis
    var ch = 0
    while (ch < meters.length) {
      meters(ch).holdDuration = millis
      ch += 1
    }
  }

  def holdDuration: Int = holdDurationVar

  def clearMeter(): Unit = {
    var ch = 0
    while (ch < meters.length) {
      meters(ch).clearMeter()
      ch += 1
    }
  }

  def clearHold(): Unit = {
    var ch = 0
    while (ch < meters.length) {
      meters(ch).clearHold()
      ch += 1
    }
  }

  def dispose(): Unit = {
    var ch = 0
    while (ch < meters.length) {
      meters(ch).dispose()
      ch += 1
    }
  }

  // --------------- public methods ---------------

  def borderVisible_=(b: Boolean): Unit = if (borderVisibleVar != b) {
    borderVisibleVar = b
    updateBorders()
  }

  def borderVisible: Boolean = borderVisibleVar

  def caption_=(b: Boolean): Unit = if (b != caption) {
    captionComp = if (b) {
      val c = new PeakMeterCaption(orientVar)
      c.setFont(getFont)
      c.setVisible(captionVisibleVar)
      c.horizontalAlignment = captionAlign
      c.labelsVisible       = captionLabelsVar
      c
    } else null

    rebuildMeters()
  }

  def caption: Boolean = captionComp != null

  def captionPosition_=(pos: Int): Unit = if (captionPositionVar != pos) {
    captionAlign = (pos: @switch) match {
      case LEFT   => RIGHT
      case RIGHT  => LEFT
      case CENTER => CENTER
      case _      => throw new IllegalArgumentException(pos.toString)
    }

    captionPositionVar = pos

    if (caption) {
      captionComp.horizontalAlignment = captionAlign
      rebuildMeters()
    }
  }

  def captionPosition: Int = captionPositionVar

  def captionLabels_=(b: Boolean): Unit = if (captionLabelsVar != b) {
    captionLabelsVar = b
    if (caption) {
      captionComp.labelsVisible = captionLabelsVar
    }
  }

  def captionLabels: Boolean = captionLabelsVar

  def captionVisible_=(b: Boolean): Unit = if (captionVisibleVar != b) {
    captionVisibleVar = b
    if (caption) {
      captionComp.setVisible(captionVisibleVar)
      updateBorders()
    }
  }

  def captionVisible: Boolean = captionVisibleVar

  def numChannels_=(num: Int): Unit = if (numChannelsVar != num) {
    numChannelsVar = num
    rebuildMeters()
  }

  def numChannels: Int = numChannelsVar

  override def paintComponent(g: Graphics): Unit = {
    super.paintComponent(g)

    getInsets(ins)
    g.setColor(Color.black)
    g.fillRect(ins.left, ins.top,
      getWidth  - (ins.left + ins.right),
      getHeight - (ins.top  + ins.bottom))
  }

  // -------------- private methods --------------

  private def rebuildMeters(): Unit = {
    removeAll()

    //    val b1 = if (caption)
    //      BorderFactory.createEmptyBorder(captionComp.ascent, 1, captionComp.descent, 1)
    //    else
    //      null

    val b2 = if (caption)
      BorderFactory.createEmptyBorder(captionComp.ascent, 1, captionComp.descent, 0)
    else
      BorderFactory.createEmptyBorder(1, 1, if (vertical) 1 else 0, if (vertical) 0 else 1)

//		val s1 = if (!borderVisibleVar || (captionVisibleVar && captionPositionVar == RIGHT)) numChannelsVar - 1 else -1
//		val s2 = if (captionVisibleVar && captionPositionVar == CENTER) numChannelsVar >> 1 else -1

    val newMeters = new Array[PeakMeterBar](numChannels)
    val numChans  = numChannelsVar
    var ch = 0
    while (ch < numChans) {
      val m           = new PeakMeterBar(orientVar)
      m.refreshParent = true
      m.rmsPainted    = rmsPaintedVar
      m.holdPainted   = holdPaintedVar
//      if ((ch == s1) || (ch == s2)) {
//        if (b1 != null) m.setBorder(b1)
//      } else {
        m.setBorder(b2)
//      }
      m.ticks = ticksVar // if( caption != null ) ticksVar else 0
      add(m)
      newMeters(ch) = m
      ch += 1
    }
    if (caption) {
      captionPositionVar match {
			   case LEFT   => add(captionComp, 0)
			   case RIGHT  => add(captionComp)
			   case CENTER => add(captionComp, getComponentCount >> 1)
			}
		}
		meters = newMeters
		revalidate()
		repaint()
	}

	private def updateBorders(): Unit = {
    // top left bottom right
    val b0 = if (borderVisibleVar)
      new RecessedBorder()
    else
      BorderFactory.createMatteBorder(0, 0, if (vertical) 0 else 1, if (vertical) 1 else 0, Color.black)
    setBorder(b0)

    //
    //		val b1		= if(caption)
    //      BorderFactory.createEmptyBorder(captionComp.ascent, 1, captionComp.descent, 1)
    //    else
    //      BorderFactory.createEmptyBorder(1, 1, 1, 1)

    val b2 = if (caption)
      BorderFactory.createEmptyBorder(captionComp.ascent, 1, captionComp.descent, 0)
    else
      BorderFactory.createEmptyBorder(1, 1, if (vertical) 1 else 0, if (vertical) 0 else 1)

    //    val s1 = if (!borderVisibleVar || (captionVisibleVar && captionPositionVar == RIGHT)) numChannelsVar - 1 else -1
    //		val s2 = if (captionVisibleVar && captionPositionVar == CENTER) numChannelsVar >> 1 else -1

    var ch = 0
    while (ch < numChannelsVar) {
      // val b = if ((ch == s1) || (ch == s2)) b1 else b2
      meters(ch).setBorder(b2) // (b)
      ch += 1
    }
  }
}
