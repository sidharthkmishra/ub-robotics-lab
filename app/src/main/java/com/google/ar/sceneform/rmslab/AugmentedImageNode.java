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
import android.util.Log;

import com.google.ar.core.AugmentedImage;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;

import java.util.concurrent.CompletableFuture;

/**
 * Node for rendering an augmented image. The image is framed by placing the virtual picture frame
 * at the corners of the augmented image trackable.
 */
@SuppressWarnings({"AndroidApiChecker"})
public class AugmentedImageNode extends AnchorNode {

  private static final String TAG = "AugmentedImageNode";

  // The augmented image represented by this node.
  private AugmentedImage image;

  private Context context;

  private ModelRenderable modelFutureRenderable;

  public AugmentedImageNode(Context context) {
    this.context = context;
  }

  /**
   * Called when the AugmentedImage is detected and should be rendered. A Sceneform node tree is
   * created based on an Anchor created from the image. The corners are then positioned based on the
   * extents of the image. There is no need to worry about world coordinates since everything is
   * relative to the center of the image, which is the parent node of the corners.
   */
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  public void setImage(AugmentedImage image) {
    this.image = image;

    // If any of the models are not loaded, then recurse when all are loaded.
    if (!MainActivity.getActivity().getRenderableMap().get(image.getIndex()).isDone()) {
      CompletableFuture.allOf(MainActivity.getActivity().getRenderableMap().get(image.getIndex()))
              .thenAccept((Void aVoid) -> setImage(image))
              .exceptionally(
                      throwable -> {
                        Log.e(TAG, "Exception loading", throwable);
                        return null;
                      });
    }

    // Set the anchor based on the center of the image.
    setAnchor(image.createAnchor(image.getCenterPose()));

    // Create the transformable andy and add it to the anchor.
    CustomizedTransformableNode andy = new CustomizedTransformableNode(context, MainActivity.getActivity().arFragment.getTransformationSystem());
    andy.setMsg(MainActivity.getActivity().getNodeMsg().get(image.getIndex()));
    andy.setParent(this);
    modelFutureRenderable = MainActivity.getActivity().getRenderableMap().get(image.getIndex()).getNow(null);
    andy.setRenderable(modelFutureRenderable);
    andy.select();
  }

  public AugmentedImage getImage() {
    return image;
  }

  public ModelRenderable getModelFutureRenderable() {
    return modelFutureRenderable;
  }
}
