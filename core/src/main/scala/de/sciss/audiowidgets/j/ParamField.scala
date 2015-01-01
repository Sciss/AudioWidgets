package de.sciss.audiowidgets.j

import javax.swing.{JFormattedTextField, JPanel}

class ParamField extends JPanel {
  init()

  private def init(): Unit = {
    val ggJog			= new Jog()
    val ggNumber	= new JFormattedTextField()
    val lbUnit		= new UnitLabel()

  }
}
