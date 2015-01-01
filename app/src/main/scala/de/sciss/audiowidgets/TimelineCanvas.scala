/*
 *  TimelineCanvas.scala
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

import scala.swing.Component

/** A component for viewing a timeline. */
trait TimelineCanvas {
  /** The underlying model */
  def timelineModel: TimelineModel

  /** The corresponding Swing component */
  def canvasComponent: Component

  /** Converts a model frame position to screen pixel location */
  def frameToScreen(frame: Long): Double

  /** Converts a screen pixel location to model frame position */
  def screenToFrame(screen: Int): Double

  /** Clips a model frame to the visible span */
  def clipVisible(frame: Double): Long
}