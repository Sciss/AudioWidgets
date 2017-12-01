/*
 *  AudioWidgets.scala
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

import java.awt.Color
import java.awt.event.{ActionEvent, ActionListener}
import javax.swing.Timer

import de.sciss.submin.Submin

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.swing.Swing._
import scala.swing.event.{ValueChanged, WindowClosing, WindowOpened}
import scala.swing.{BorderPanel, BoxPanel, Component, Frame, GridPanel, Label, MainFrame, Orientation, SimpleSwingApplication}

object Demo extends SimpleSwingApplication {

  override def startup(args: Array[String]): Unit = {
    Submin.install(args.contains("--dark"))
    super.startup(args)
  }

  lazy val top: Frame = new MainFrame {
    title = "ScalaAudioWidgets"

    val m: PeakMeter = new PeakMeter {
      numChannels   = 2
      ticks         = 101 // 50
      caption    = true
      borderVisible = true
    }

    val lcdColors = Vec(
      (Some(Color.darkGray)             , None),
      (Some(new Color(205, 232, 254))   , Some(new Color(15, 42, 64))),
      (Some(Color.darkGray)             , Some(Color.lightGray)),
      (Some(new Color(60, 30, 20))      , Some(new Color(200, 100, 100))),
      (Some(new Color(0xE0, 0xE0, 0xE0)), Some(new Color(0x20, 0x20, 0x20)))
    )

    val lcdLbs: Seq[Label] = lcdColors.zipWithIndex.map {
      case ((fg, _), idx) =>
        new Label {
          text = s"00:00:0$idx"
          if (idx != 1 && idx != 4) peer.putClientProperty("styleId", "noshade")
          peer.putClientProperty("JComponent.sizeVariant", "small")
          font = LCDFont()
          fg.foreach(foreground = _)

          //          override protected def paintComponent(g: _root_.scala.swing.Graphics2D): Unit = {
          //            import java.awt.RenderingHints
          //            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
          //            // g.setRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST, 250)
          //            // g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HBGR)
          //            // g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
          //            // g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
          //            super.paintComponent(g)
          //          }
        }
    }

    val lcds: Seq[LCDPanel] = lcdColors.zip(lcdLbs).map {
      case ((_, bg), lb) =>
        new LCDPanel {
          bg.foreach(background = _)
          contents += lb
        }
    }
    val lcdGrid: GridPanel = new GridPanel(lcdColors.size, 1) {
      vGap = 6
      contents ++= lcds
    }

    val axis: Axis = new Axis {
      format  = AxisFormat.Time()
      minimum = 0.0
      maximum = 34.56
    }

    lazy val trnspActions: Seq[Transport.ActionElement] = Seq(
      Transport.GoToBegin, Transport.Rewind     , Transport.Stop   , Transport.Pause, Transport.Play,
      Transport.Record   , Transport.FastForward, Transport.GoToEnd, Transport.Loop).map {

      case l @ Transport.Loop => l.apply {
          trnsp.button(l).foreach(b => b.selected = !b.selected)
        }

      case e => e.apply {}
    }
    lazy val trnsp: Component with Transport.ButtonStrip = {
      val res = Transport.makeButtonStrip(trnspActions)
      res.button(Transport.Play).get.selected = true // peer.getModel.setPressed(true)
      res
    }

    lazy val timeSlid: LCDPanel = {
      val fmt = AxisFormat.Time(hours = true, millis = false)
      val lb = new Label {
        text        = fmt.format(value = 0.0, decimals = 0)
        foreground  = LCDColors.foreground
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

    lazy val southPane: BoxPanel = new BoxPanel(Orientation.Vertical) {
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
      val lb1: Label = lcdLbs.head
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
