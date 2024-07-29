/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mlkit.vision.demo.kotlin.objectdetector

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.example.android.wearable.ml.vision.GraphicOverlay
import com.google.mlkit.vision.objects.DetectedObject
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Draw the detected object info in preview.  */
class ObjectGraphic constructor(
    overlay: GraphicOverlay,
    private val detectedObject: DetectedObject
) : GraphicOverlay.Graphic(overlay) {

  private val numColors = COLORS.size

  private val boxPaints = Array(numColors) { Paint() }
  private val textPaints = Array(numColors) { Paint() }
  private val labelPaints = Array(numColors) { Paint() }

  init {
    for (i in 0 until numColors) {
      textPaints[i] = Paint()
      textPaints[i].color = COLORS[i][0]
      textPaints[i].textSize = TEXT_SIZE
      boxPaints[i] = Paint()
      boxPaints[i].color = COLORS[i][1]
      boxPaints[i].style = Paint.Style.STROKE
      boxPaints[i].strokeWidth = STROKE_WIDTH
      labelPaints[i] = Paint()
      labelPaints[i].color = COLORS[i][1]
      labelPaints[i].style = Paint.Style.FILL
    }
  }

  override fun draw(canvas: Canvas) {
    // Decide color based on object tracking ID
    val colorID =
      if (detectedObject.trackingId == null) 0
      else abs(detectedObject.trackingId!! % NUM_COLORS)
    var textWidth =
      textPaints[colorID].measureText("Tracking ID: " + detectedObject.trackingId)
    val lineHeight = TEXT_SIZE + STROKE_WIDTH
    var yLabelOffset = -lineHeight

    // Calculate width and height of label box
    for (label in detectedObject.labels) {
      textWidth =
        max(textWidth, textPaints[colorID].measureText(label.text))
      textWidth = max(
        textWidth,
        textPaints[colorID].measureText(
          String.format(
            Locale.US,
            LABEL_FORMAT,
            label.confidence * 100,
            label.index
          )
        )
      )
      yLabelOffset -= 2 * lineHeight
    }

    // Draws the bounding box.
    val rect = RectF(detectedObject.boundingBox)
    val x0 = translateX(rect.left)
    val x1 = translateX(rect.right)
    rect.left = min(x0, x1)
    rect.right = max(x0, x1)
    rect.top = translateY(rect.top)
    rect.bottom = translateY(rect.bottom)
    canvas.drawRect(rect, boxPaints[colorID])

    // Draws other object info.
    canvas.drawRect(
      rect.left - STROKE_WIDTH,
      rect.top + yLabelOffset,
      rect.left + textWidth + 2 * STROKE_WIDTH,
      rect.top,
      labelPaints[colorID]
    )
    yLabelOffset += TEXT_SIZE
    canvas.drawText(
      "Tracking ID: " + detectedObject.trackingId,
      rect.left,
      rect.top + yLabelOffset,
      textPaints[colorID]
    )
    yLabelOffset += lineHeight
    for (label in detectedObject.labels) {
      canvas.drawText(
        label.text + " (index: " + label.index + ")",
        rect.left,
        rect.top + yLabelOffset,
        textPaints[colorID]
      )
      yLabelOffset += lineHeight
      canvas.drawText(
        String.format(
          Locale.US,
          LABEL_FORMAT,
          label.confidence * 100,
          label.index
        ),
        rect.left,
        rect.top + yLabelOffset,
        textPaints[colorID]
      )
      yLabelOffset += lineHeight
    }

    // Save the RuleOfThirds box.
    val RuleOfThirds_rect = Rect(Math.round((canvas.width/3).toFloat()),
            Math.round((canvas.height/3).toFloat()),
            Math.round((canvas.width*2/3).toFloat()),
            Math.round((canvas.height*2/3).toFloat()))

    //Draw RuleOfThirds lines
    canvas.drawLine(0f, RuleOfThirds_rect.top.toFloat(),
                    canvas.width.toFloat(), RuleOfThirds_rect.top.toFloat(),
                    boxPaints[0])

    canvas.drawLine(0f, RuleOfThirds_rect.bottom.toFloat(),
                    canvas.width.toFloat(), RuleOfThirds_rect.bottom.toFloat(),
                    boxPaints[0])

    canvas.drawLine(RuleOfThirds_rect.left.toFloat(), 0f,
                    RuleOfThirds_rect.left.toFloat(), canvas.height.toFloat(),
                    boxPaints[0])

    canvas.drawLine(RuleOfThirds_rect.right.toFloat(), 0f,
                    RuleOfThirds_rect.right.toFloat(), canvas.height.toFloat(),
                    boxPaints[0])

    //Calculate Detected object center:
    /*val object_center_x = rect.centerX();
    val object_center_y = rect.centerY();

    //Calculating distance from Detected object center to NorthWest corner of RuleOfThirds_rect
    val Distance_NorthWest_X = abs(rect.centerX()-RuleOfThirds_rect.left);
    val Distance_NorthWest_Y = abs(rect.centerY()-RuleOfThirds_rect.top);

    //Calculating distance from Detected object center to SouthWest corner of RuleOfThirds_rect
    val Distance_SouthWest_X = abs(rect.centerX()-RuleOfThirds_rect.left);
    val Distance_SouthWest_Y = abs(rect.centerY()-RuleOfThirds_rect.bottom);

    //Calculating distance from Detected object center to NorthEast corner of RuleOfThirds_rect
    val Distance_NorthEast_X = abs(rect.centerX()-RuleOfThirds_rect.right);
    val Distance_NorthEast_Y = abs(rect.centerY()-RuleOfThirds_rect.top);

    //Calculating distance from Detected object center to SouthEast corner of RuleOfThirds_rect
    val Distance_SouthEast_X = abs(rect.centerX()-RuleOfThirds_rect.right);
    val Distance_SouthEast_Y = abs(rect.centerY()-RuleOfThirds_rect.bottom);*/

    //Calculate which direction to move
      if(rect.centerX()<canvas.width/2){
          if(rect.centerY()>canvas.height/2){

              //Draw RuleOfThirds suggestion line to SouthWest corner
              canvas.drawLine(rect.centerX(), rect.centerY(),
                  RuleOfThirds_rect.left.toFloat(), RuleOfThirds_rect.bottom.toFloat(),
                  boxPaints[3])

          }else{

              //Draw RuleOfThirds suggestion line to NorthWest corner
              canvas.drawLine(rect.centerX(), rect.centerY(),
                  RuleOfThirds_rect.left.toFloat(), RuleOfThirds_rect.top.toFloat(),
                  boxPaints[2])

          }
      } else{
          if(rect.centerY()>canvas.height/2){

              //Draw RuleOfThirds suggestion line to SouthEast corner
              canvas.drawLine(rect.centerX(), rect.centerY(),
                  RuleOfThirds_rect.right.toFloat(), RuleOfThirds_rect.bottom.toFloat(),
                  boxPaints[5])

          }else{

              //Draw RuleOfThirds suggestion line to NorthEast corner
              canvas.drawLine(rect.centerX(), rect.centerY(),
                  RuleOfThirds_rect.right.toFloat(), RuleOfThirds_rect.top.toFloat(),
                  boxPaints[4])

          }
      }

  }

  companion object {
    private const val TEXT_SIZE = 54.0f
    private const val STROKE_WIDTH = 4.0f
    private const val NUM_COLORS = 10
    private val COLORS =
      arrayOf(
        intArrayOf(Color.BLACK, Color.WHITE),
        intArrayOf(Color.WHITE, Color.MAGENTA),
        intArrayOf(Color.BLACK, Color.LTGRAY),
        intArrayOf(Color.WHITE, Color.RED),
        intArrayOf(Color.WHITE, Color.BLUE),
        intArrayOf(Color.WHITE, Color.DKGRAY),
        intArrayOf(Color.BLACK, Color.CYAN),
        intArrayOf(Color.BLACK, Color.YELLOW),
        intArrayOf(Color.WHITE, Color.BLACK),
        intArrayOf(Color.BLACK, Color.GREEN)
      )
    private const val LABEL_FORMAT = "%.2f%% confidence (index: %d)"
  }
}
