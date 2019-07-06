/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.sceneform.rmslab;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.TextView;

import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.sceneform.ux.TransformationSystem;

import java.util.ArrayList;

/**
 * Node that represents a planet.
 *
 * <p>The planet creates two child nodes when it is activated:
 *
 * <ul>
 *   <li>The visual of the planet, rotates along it's own axis and renders the planet.
 *   <li>An info card, renders an Android View that displays the name of the planerendt. This can be
 *       toggled on and off.
 * </ul>
 *
 * The planet is rendered by a child instead of this node so that the spinning of the planet doesn't
 * make the info card spin as well.
 */
public class CustomizedTransformableNode extends TransformableNode implements Node.OnTapListener {
  private ArrayList<String> msg = new ArrayList<>();

  private int msgIndex = 0;

  private TextView textView;

  public void setMsg(ArrayList<String> msg) {
    this.msg = msg;
  }

  private String getMsg() {
    String strMsg = "Hi!";
    if(msg != null && msg.size() > 0) {
      strMsg = msg.get(msgIndex);
      msgIndex = (msgIndex + 1) % msg.size();
      msgIndex = msgIndex == 0? 1 : msgIndex;
    }
    return strMsg;
  }

  private Node infoCard;
  private final Context context;

  public CustomizedTransformableNode(
          Context context,
          TransformationSystem trfSystem) {
    super(trfSystem);
    this.context = context;
    setOnTapListener(this);
  }

  @Override
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  public void onActivate() {
    super.onActivate();
    if (getScene() == null) {
      throw new IllegalStateException("Scene is null!");
    }

    if (infoCard == null) {
      infoCard = new Node();
      infoCard.setParent(this);
      infoCard.setEnabled(true);
      infoCard.setLocalPosition(new Vector3(0.5f, 0.5f, 0.5f));

      ViewRenderable.builder()
          .setView(context, R.layout.planet_card_view)
          .build()
          .thenAccept(
              (renderable) -> {
                infoCard.setRenderable(renderable);
                textView = (TextView) renderable.getView();
                textView.setText(getMsg());
              })
          .exceptionally(
              (throwable) -> {
                throw new AssertionError("Could not load plane card view.", throwable);
              });
    }

  }

  @Override
  public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
    if (infoCard == null) {
      return;
    }

    textView.setText(getMsg());
    infoCard.setEnabled(!infoCard.isEnabled());
    super.onTap(hitTestResult,  motionEvent);
  }

}
