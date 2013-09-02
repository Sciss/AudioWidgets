/*
 *  AudioWidgets.scala
 *  (AudioWidgets)
 *
 *  Copyright (c) 2011-2013 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License
 *	as published by the Free Software Foundation; either
 *	version 2, june 1991 of the License, or (at your option) any later version.
 *
 *	This software is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License (gpl.txt) along with this software; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.audiowidgets

import java.awt.event.{ActionEvent, ActionListener}
import scala.swing.event.{ValueChanged, WindowClosing, WindowOpened}
import collection.immutable.{IndexedSeq => Vec}
import java.awt.Color
import scala.swing.{Orientation, BoxPanel, Component, Label, GridPanel, Swing, BorderPanel, MainFrame, SimpleSwingApplication}
import javax.swing.Timer
import Swing._

object Demo extends SimpleSwingApplication {
  lazy val top = new MainFrame {
    title = "ScalaAudioWidgets"
    peer.getRootPane.putClientProperty("apple.awt.brushMetalLook", java.lang.Boolean.TRUE)

    val m = new PeakMeter {
      numChannels   = 2
      ticks         = 101 // 50
      hasCaption    = true
      borderVisible = true
    }

    val lcdColors   = Vec(
      (Some(Color.darkGray)             , None),
      (Some(new Color(205, 232, 254))   , Some(new Color(15, 42, 64))),
      (Some(Color.darkGray)             , Some(Color.lightGray)),
      (Some(new Color(60, 30, 20))      , Some(new Color(200, 100, 100))),
      (Some(new Color(0xE0, 0xE0, 0xE0)), Some(new Color(0x20, 0x20, 0x20)))
    )

    val lcdLbs = lcdColors.zipWithIndex.map {
      case ((fg, _), idx) =>
        new Label {
          text = "00:00:0" + idx
          peer.putClientProperty("JComponent.sizeVariant", "small")
          fg.foreach(foreground = _)
        }
    }

    val lcds = lcdColors.zip(lcdLbs).zipWithIndex.map {
      case (((fg, bg), lb), idx) =>
        new LCDPanel {
          bg.foreach(background = _)
          contents += lb
        }
    }
    val lcdGrid = new GridPanel(lcdColors.size, 1) {
      vGap = 6
      contents ++= lcds
    }

    val axis = new Axis {
      format = AxisFormat.Time()
      minimum = 0.0
      maximum = 34.56
    }

    lazy val trnspActions = Seq(
      Transport.GoToBegin, Transport.Play, Transport.Stop, Transport.GoToEnd, Transport.Loop).map {

      case l @ Transport.Loop => l.apply {
          trnsp.button(l).foreach(b => b.selected = !b.selected)
        }

      case e => e.apply {}
    }
    lazy val trnsp: Component with Transport.ButtonStrip = Transport.makeButtonStrip(trnspActions)

    lazy val timeSlid = {
      val fmt = AxisFormat.Time(hours = true, millis = false)
      val lb = new Label {
        text        = fmt.format(value = 0.0, decimals = 0)
        foreground  = Color.darkGray
        peer.putClientProperty("JComponent.sizeVariant", "small")
      }
      val slid = new DualRangeSlider(DualRangeModel(0, 360 * 4)) { me =>
        listenTo(this)
        border = EmptyBorder(4, 0, 0, 0)
        reactions += {
          case ValueChanged(_) => lb.text = fmt.format(value = me.value.toDouble * 0.25, decimals = 0)
        }
      }
      new LCDPanel {
        vGap      = 0
        contents += lb
        contents += slid
      }
    }

    lazy val southPane = new BoxPanel(Orientation.Vertical) {
      contents += trnsp
      contents += VStrut(6)
      contents += timeSlid
    }

    contents = new BorderPanel {
      import BorderPanel.Position._

      add(m, West)
      add(new BorderPanel {
        add(lcdGrid, North)
        border = EmptyBorder(0, 20, 0, 0)
      }, East)
      add(new BorderPanel {
        add(axis, North)
        border = EmptyBorder(0, 0, 20, 0)
      }, North)
      add(southPane, South)
      border = EmptyBorder(20, 20, 20, 20)
    }

    val t = new Timer(30, new ActionListener {
      val rnd   = new util.Random()
      var peak1 = 0.5f
      var rms1  = 0f
      var peak2 = 0.5f
      var rms2  = 0f

      def actionPerformed(e: ActionEvent): Unit = {
        peak1 = math.max(0f, math.min(1f, peak1 + math.pow(rnd.nextFloat() * 0.5, 2).toFloat * (if (rnd.nextBoolean()) 1 else -1)))
        rms1  = math.max(0f, math.min(peak1, rms1 * 0.98f + (rnd.nextFloat() * 0.02f * (if (rnd.nextBoolean()) 1 else -1))))
        peak2 = math.max(0f, math.min(1f, peak2 + math.pow(rnd.nextFloat() * 0.5, 2).toFloat * (if (rnd.nextBoolean()) 1 else -1)))
        rms2  = math.max(0f, math.min(peak2, rms2 * 0.98f + (rnd.nextFloat() * 0.02f * (if (rnd.nextBoolean()) 1 else -1))))
        m.update(Vec(peak1, rms1, peak2, rms2))
      }
    })

    val t2 = new Timer(1000, new ActionListener {
      val lb1 = lcdLbs.head
      var cnt = 0

      def actionPerformed(e: ActionEvent): Unit = {
        cnt += 1
        val secs  = cnt % 60
        val mins  = (cnt / 60) % 60
        val hours = (cnt / 3600) % 100
        lb1.text = (hours + 100).toString.substring(1) + ":" +
                   (mins  + 100).toString.substring(1) + ":" +
                   (secs  + 100).toString.substring(1)
      }
    })
    reactions += {
      case WindowOpened (_) => t.start(); t2.start()
      case WindowClosing(_) => t.stop (); t2.stop ()
    }
  }
}
