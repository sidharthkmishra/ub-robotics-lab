/*
 * Copyright 2018 Google LLC. All Rights Reserved.
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

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.CamcorderProfile;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.animation.ModelAnimator;
import com.google.ar.sceneform.rendering.AnimationData;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
public class MainActivity extends AppCompatActivity {
  private static final String TAG = MainActivity.class.getSimpleName();
  private static final double MIN_OPENGL_VERSION = 3.0;

  private static final Integer ANDY_INDEX = 0;
  private static final Integer DEPTH_DRONE_INDEX = 1;
  private static final Integer DRONE_INDEX = 2;
  private static final Integer EARTH_INDEX = 3;
  private static final Integer IRON_INDEX = 4;

  // Augmented image and its associated center pose anchor, keyed by the augmented image in
  // the database.
  private final Map<AugmentedImage, AugmentedImageNode> augmentedImageMap = new HashMap<>();

  public Map<Integer, ArrayList<String>> getNodeMsg() {
    return nodeMsg;
  }

  private Map<Integer, ArrayList<String>> nodeMsg = new HashMap<>();

  private HashMap<Integer, CompletableFuture<ModelRenderable>> renderableMap = new HashMap<>();

  public HashMap<Integer, CompletableFuture<ModelRenderable> > getRenderableMap() {
    return renderableMap;
  }

  public ArFragment arFragment;

  // VideoRecorder encapsulates all the video recording functionality.
  private VideoRecorder videoRecorder;

  // Controls animation playback.
  private ModelAnimator animator;

  private int nextAnimation;

  // The UI to record.
  private FloatingActionButton recordButton;
  private FloatingActionButton cameraToggleButton;

  private Snackbar loadingMessageSnackbar = null;

  private ImageView fitToScanView;

  private static MainActivity thisActivity;

  public static MainActivity getActivity() {
    return thisActivity;
  }

  @Override
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  // CompletableFuture requires api level 24
  // FutureReturnValueIgnored is not valid
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    thisActivity = this;

    if (!checkIsSupportedDeviceOrFinish(this)) {
      return;
    }

    setContentView(R.layout.activity_ux);
    //Load all the 3d models
    loadModels();

    arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
    fitToScanView = findViewById(R.id.image_view_fit_to_scan);

    arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);

    // Initialize the VideoRecorder.
    videoRecorder = new VideoRecorder();
    int orientation = getResources().getConfiguration().orientation;
    videoRecorder.setVideoQuality(CamcorderProfile.QUALITY_2160P, orientation);
    videoRecorder.setSceneView(arFragment.getArSceneView());

    recordButton = findViewById(R.id.record);
    recordButton.setOnClickListener(this::toggleRecording);
    recordButton.setEnabled(true);
    recordButton.setImageResource(R.drawable.round_videocam);

    cameraToggleButton = findViewById(R.id.cameratoggle);
    cameraToggleButton.setOnClickListener(this::toggleCamera);
    cameraToggleButton.setEnabled(true);

    findViewById(R.id.animate).setOnClickListener(this::onPlayAnimation);

  }

  private void loadModels() {
    CompletableFuture<ModelRenderable> andyFutureRenderable;
    andyFutureRenderable =
            ModelRenderable.builder()
                    .setSource(this, R.raw.andy_dance)
                    .build();
    renderableMap.put(ANDY_INDEX, andyFutureRenderable);
    nodeMsg.put(ANDY_INDEX, new ArrayList<>());
    nodeMsg.get(ANDY_INDEX).add("Hi!");
    nodeMsg.get(ANDY_INDEX).add("Welcome to CSE 611");
    nodeMsg.get(ANDY_INDEX).add("I'm Andy!");
    nodeMsg.get(ANDY_INDEX).add("m cool :)");
    nodeMsg.get(ANDY_INDEX).add("Wassup?");
    nodeMsg.get(ANDY_INDEX).add("Android - modified version of Linux");
    nodeMsg.get(ANDY_INDEX).add("Latest release: 9.0 Pie");
    nodeMsg.get(ANDY_INDEX).add("ll meet soon");
    nodeMsg.get(ANDY_INDEX).add("byeeee");

    andyFutureRenderable =
            ModelRenderable.builder()
                    .setSource(this, R.raw.depth_drone)
                    .build();
    renderableMap.put(DEPTH_DRONE_INDEX, andyFutureRenderable);
    nodeMsg.put(DEPTH_DRONE_INDEX, new ArrayList<>());
    nodeMsg.get(DEPTH_DRONE_INDEX).add("Hi!");
    nodeMsg.get(DEPTH_DRONE_INDEX).add("Its a Deep Drone");
    nodeMsg.get(DEPTH_DRONE_INDEX).add("Submersible Remotely operated vehicle");
    nodeMsg.get(DEPTH_DRONE_INDEX).add("Designed for mid-water salvage for US Navy");
    nodeMsg.get(DEPTH_DRONE_INDEX).add("Operated by Phoenix International");
    nodeMsg.get(DEPTH_DRONE_INDEX).add("Length: 9 ft 3 in");
    nodeMsg.get(DEPTH_DRONE_INDEX).add("Width: 4 ft 7 in");
    nodeMsg.get(DEPTH_DRONE_INDEX).add("Height: 6 ft 2 in");
    nodeMsg.get(DEPTH_DRONE_INDEX).add("Maximum Depth: 8,000 ft (2,440 m)");

    andyFutureRenderable =
            ModelRenderable.builder()
                    .setSource(this, R.raw.drone)
                    .build();
    renderableMap.put(DRONE_INDEX, andyFutureRenderable);
    nodeMsg.put(DRONE_INDEX, new ArrayList<>());
    nodeMsg.get(DRONE_INDEX).add("Hi!");
    nodeMsg.get(DRONE_INDEX).add("Drone used by Police");
    nodeMsg.get(DRONE_INDEX).add("Be careful");
    nodeMsg.get(DRONE_INDEX).add("Designed carefully");
    nodeMsg.get(DRONE_INDEX).add("Loaded with sophisticated camera");
    nodeMsg.get(DRONE_INDEX).add("Danger!");

    andyFutureRenderable =
            ModelRenderable.builder()
                    .setSource(this, R.raw.globe)
                    .build();
    renderableMap.put(EARTH_INDEX, andyFutureRenderable);
    nodeMsg.put(EARTH_INDEX, new ArrayList<>());
    nodeMsg.get(EARTH_INDEX).add("Hi!");
    nodeMsg.get(EARTH_INDEX).add("m mother earth");
    nodeMsg.get(EARTH_INDEX).add("Third planet from the Sun ");
    nodeMsg.get(EARTH_INDEX).add("I'm beautiful");
    nodeMsg.get(EARTH_INDEX).add("Formed over 4.5 billion years ago");
    nodeMsg.get(EARTH_INDEX).add("Only natural satellite.");
    nodeMsg.get(EARTH_INDEX).add("Revolves around the Sun in 365.26 days");
    nodeMsg.get(EARTH_INDEX).add("71% is covered with water");
    nodeMsg.get(EARTH_INDEX).add("Over the next 3.5 Bys, solar luminosity will increase by 40%");

    andyFutureRenderable =
            ModelRenderable.builder()
                    .setSource(this, R.raw.iron_man)
                    .build();
    renderableMap.put(IRON_INDEX, andyFutureRenderable);
    nodeMsg.put(IRON_INDEX, new ArrayList<>());
    nodeMsg.get(IRON_INDEX).add("Hi!");
    nodeMsg.get(IRON_INDEX).add("Everyone knows me :)");
    nodeMsg.get(IRON_INDEX).add("A billionaire industrialist and genius inventor");
    nodeMsg.get(IRON_INDEX).add("Tony Stark");
    nodeMsg.get(IRON_INDEX).add("Terrorists kidnap me");
    nodeMsg.get(IRON_INDEX).add("and guess what :)");
    nodeMsg.get(IRON_INDEX).add("I built this armored suit there :)");
    nodeMsg.get(IRON_INDEX).add("member of team Avengers");
  }

  private void onUpdateFrame(FrameTime frameTime) {
    Frame frame = arFragment.getArSceneView().getArFrame();

    // If there is no frame or ARCore is not tracking yet, just return.
    if (frame == null || frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
      return;
    }

    Collection<AugmentedImage> updatedAugmentedImages =
            frame.getUpdatedTrackables(AugmentedImage.class);
    for (AugmentedImage augmentedImage : updatedAugmentedImages) {
      switch (augmentedImage.getTrackingState()) {
        case PAUSED:
          // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
          // but not yet tracked.
          //String text = "Detected Image " + augmentedImage.getIndex();
          //SnackbarHelper.getInstance().showMessage(this, text);
          break;

        case TRACKING:
          // Have to switch to UI Thread to update View.
          fitToScanView.setVisibility(View.GONE);

          // Create a new anchor for newly found images.
          if (!augmentedImageMap.containsKey(augmentedImage)) {
            AugmentedImageNode node = new AugmentedImageNode(this);
            node.setImage(augmentedImage);
            augmentedImageMap.put(augmentedImage, node);
            arFragment.getArSceneView().getScene().addChild(node);
          }
          break;

        case STOPPED:
          augmentedImageMap.remove(augmentedImage);
          break;
      }
    }
  }

  private void onPlayAnimation(View unusedView) {
    if(augmentedImageMap.size() == 0)
      return;

    for(Map.Entry<AugmentedImage, AugmentedImageNode> entry : augmentedImageMap.entrySet()) {
      if(entry.getKey().getIndex() == ANDY_INDEX) {
        ModelRenderable andyRenderable = entry.getValue().getModelFutureRenderable();
        if (animator == null || !animator.isRunning()) {
          AnimationData data = andyRenderable.getAnimationData(nextAnimation);
          nextAnimation = (nextAnimation + 1) % andyRenderable.getAnimationDataCount();
          animator = new ModelAnimator(data, andyRenderable);
          animator.start();
          Toast toast = Toast.makeText(this, data.getName(), Toast.LENGTH_SHORT);
          Log.d(
                  TAG,
                  String.format(
                          "Starting animation %s - %d ms long", data.getName(), data.getDurationMs()));
          toast.setGravity(Gravity.CENTER, 0, 0);
          toast.show();
        }
      }
    }
  }

  @Override
  protected void onPause() {
    //if (videoRecorder.isRecording()) {
    //  toggleRecording(null);
    //}
    super.onPause();
  }

  public boolean hasWritePermission() {
    return ActivityCompat.checkSelfPermission(
            arFragment.requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED;
  }

  /** Launch Application Setting to grant permissions. */
  public void launchPermissionSettings() {
    Intent intent = new Intent();
    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    intent.setData(Uri.fromParts("package", arFragment.requireActivity().getPackageName(), null));
    arFragment.requireActivity().startActivity(intent);
  }
  /*
   * Used as a handler for onClick, so the signature must match onClickListener.
   */
  private void toggleRecording(View unusedView) {
    if (!hasWritePermission()) {
      Log.e(TAG, "Video recording requires the WRITE_EXTERNAL_STORAGE permission");
      Toast.makeText(
              this,
              "Video recording requires the WRITE_EXTERNAL_STORAGE permission",
              Toast.LENGTH_LONG)
          .show();
      launchPermissionSettings();
      return;
    }
    boolean recording = videoRecorder.onToggleRecord();
    if (recording) {
      recordButton.setImageResource(R.drawable.round_stop);
    } else {
      recordButton.setImageResource(R.drawable.round_videocam);
      String videoPath = videoRecorder.getVideoPath().getAbsolutePath();
      Toast.makeText(this, "Video saved: " + videoPath, Toast.LENGTH_SHORT).show();
      Log.d(TAG, "Video saved: " + videoPath);

      // Send  notification of updated content.
      ContentValues values = new ContentValues();
      values.put(MediaStore.Video.Media.TITLE, "Sceneform Video");
      values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
      values.put(MediaStore.Video.Media.DATA, videoPath);
      getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    // TODO Auto-generated method stub
    super.onSaveInstanceState(outState);
  }

  private void toggleCamera(View unusedView) {
    Intent intent = new Intent(getApplicationContext(), AugmentedFacesActivity.class);
    startActivity(intent);
    //finish();
  }

  /**
   * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
   * on this device.
   *
   * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
   *
   * <p>Finishes the activity if Sceneform can not run
   */
  public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
    if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
      Log.e(TAG, "Sceneform requires Android N or later");
      Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
      activity.finish();
      return false;
    }
    String openGlVersionString =
        ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
            .getDeviceConfigurationInfo()
            .getGlEsVersion();
    if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
      Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
      Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
          .show();
      activity.finish();
      return false;
    }
    return true;
  }

  void onException(int id, Throwable throwable) {
    Toast toast = Toast.makeText(this, "Unable to load renderable: " + id, Toast.LENGTH_LONG);
    toast.setGravity(Gravity.CENTER, 0, 0);
    toast.show();
    Log.e(TAG, "Unable to load andy renderable", throwable);
  }
  private void showLoadingMessage() {
    if (loadingMessageSnackbar != null && loadingMessageSnackbar.isShownOrQueued()) {
      return;
    }

    loadingMessageSnackbar =
            Snackbar.make(
                    MainActivity.this.findViewById(android.R.id.content),
                    R.string.plane_finding,
                    Snackbar.LENGTH_INDEFINITE);
    loadingMessageSnackbar.getView().setBackgroundColor(0xbf323232);
    loadingMessageSnackbar.show();
  }

  private void hideLoadingMessage() {
    if (loadingMessageSnackbar == null) {
      return;
    }

    loadingMessageSnackbar.dismiss();
    loadingMessageSnackbar = null;
  }

}
