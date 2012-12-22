package de.sciss.gui.j

import javax.swing.{JComponent, Action}

trait InstallableAction extends Action {
   def install( component: JComponent, condition: Int = JComponent.WHEN_IN_FOCUSED_WINDOW ) : Unit
}