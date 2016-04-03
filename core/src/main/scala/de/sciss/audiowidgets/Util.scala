/*
 *  Util.scala
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

import javax.swing.UIManager

object Util {
  private[this] val isDark = UIManager.getBoolean("dark-skin")

  def isDarkSkin: Boolean = isDark
}
