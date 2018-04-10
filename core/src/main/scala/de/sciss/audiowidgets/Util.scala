/*
 *  Util.scala
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

import java.awt.Color
import javax.swing.UIManager

object Util {
  final val isMac		  : Boolean = sys.props("os.name").contains("Mac OS")
  final val isWindows	: Boolean = sys.props("os.name").contains("Windows")
  final val isLinux		: Boolean = !(isMac || isWindows) 	// Well...
  final val needsSync : Boolean = isLinux && sys.props("java.version").startsWith("1.8.")
  final val isDarkSkin: Boolean = UIManager.getBoolean("dark-skin")

  def colrSelection         : Color = if (isDarkSkin) colrSelectionDark  else colrSelectionLight
  def colrInactiveSelection : Color = if (isDarkSkin) colrSelectionDarkI else colrSelectionLightI

  private[this] final val colrSelectionDark   = new Color(95, 142, 255, 0x38)
  private[this] final val colrSelectionLight  = new Color(0x00, 0x00, 0xFF, 0x2F)
  private[this] final val colrSelectionDarkI  = new Color(0xE0, 0xE0, 0xE0, 0x30)
  private[this] final val colrSelectionLightI = new Color(0x00, 0x00, 0x00, 0x20)
}