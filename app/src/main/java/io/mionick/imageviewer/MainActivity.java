package io.mionick.imageviewer;

import static io.mionick.imageviewer.ui.ButtonId.*;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import io.mionick.imageviewer.gestures.RotationGestureDetector;
import io.mionick.imageviewer.ui.ButtonId;
import io.mionick.imageviewer.ui.ButtonInfo;

public class MainActivity extends AppCompatActivity {

    private static final int MENU_DISPLAYED_MARGIN = 0;
    public static final int PICK_IMAGE = 1;
    public static final String TAG = "LOOKHERE";

    private String imageUri;
    private CustomCanvas customCanvas;
    // Scale variables
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private RotationGestureDetector rotationGestureDetector;
    private boolean rotationDisabled = false;
    private boolean menuVisible = true;
    private ViewGroup topLevelMenu;
    private final Map<ButtonId, ButtonInfo> menuButtons = new LinkedHashMap<>();
    private boolean deformMode = false;
    private boolean showGrid = false;
    boolean deformOngoing = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RelativeLayout mainLayout = findViewById(R.id.root);
        topLevelMenu = findViewById(R.id.menu);
        customCanvas = new CustomCanvas(this);
        mainLayout.addView(customCanvas, 1);
        customCanvas.setBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.mionick_logo_lg));

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
        gestureDetector = new GestureDetector(this, new GestureListener());
        rotationGestureDetector = new RotationGestureDetector(new RotationListener());


        // Building Menu
        menuButtons.put(OPEN_IMAGE, new ButtonInfo(android.R.drawable.ic_input_add,
                OPEN_IMAGE.toString(),
                view -> {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
                }));

        menuButtons.put(LOCK_ROTATION, new ButtonInfo(android.R.drawable.ic_menu_rotate,
                LOCK_ROTATION.toString(),
                view -> {
                    rotationDisabled = !rotationDisabled;
                    view.setBackgroundTintList(ColorStateList.valueOf(getColor(!rotationDisabled ? R.color.colorPrimary : R.color.colorAccent)));
                }));
        menuButtons.put(RESET_TRANSFORM, new ButtonInfo(android.R.drawable.ic_menu_revert,
                RESET_TRANSFORM.toString(),
                view -> customCanvas.resetImageTransform()));

        menuButtons.put(DEFORM, new ButtonInfo(android.R.drawable.ic_menu_directions,
                DEFORM.toString(),
                view -> {
                    deformMode = !deformMode;
                    view.setBackgroundTintList(ColorStateList.valueOf(getColor(!deformMode ? R.color.colorPrimary : R.color.colorAccent)));
                    customCanvas.setDeformHandlesVisible(deformMode);
                }));

        menuButtons.put(SHOW_GRID, new ButtonInfo(android.R.drawable.ic_dialog_dialer,
                SHOW_GRID.toString(),
                view -> {
                    showGrid = !showGrid;
                    view.setBackgroundTintList(ColorStateList.valueOf(getColor(!showGrid ? R.color.colorPrimary : R.color.colorAccent)));
                    customCanvas.setGridVisible(showGrid);
                }));
//
//        menuButtons.put(TEST, new ButtonInfo(android.R.drawable.ic_secure,
//                TEST.toString(),
//                view -> {
//                    float[] cornersInScreenSpace = customCanvas.getCornersInScreenSpace();
//                    cornersInScreenSpace[0] += 100;
//                    customCanvas.setDeformation(cornersInScreenSpace);
//                }));

        menuButtons.put(HIDE_MENU, new ButtonInfo(android.R.drawable.ic_menu_more,
                HIDE_MENU.toString(),
                new OnClickListener() {
                    int newMargin = 0;
                    int oldMargin = 0;
                    final Animation a = new Animation() {
                        @Override
                        protected void applyTransformation(float interpolatedTime, Transformation t) {
                            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) topLevelMenu.getLayoutParams();
                            params.setMargins(oldMargin + (int) ((newMargin - oldMargin) * interpolatedTime), 0, 0, 0);
                            topLevelMenu.setLayoutParams(params);
                        }
                    };

                    @Override
                    public void onClick(View v) {
                        menuVisible = !menuVisible;
                        if (menuVisible) {
                            oldMargin = -(v.getWidth() * (menuButtons.size() - 1));
                            newMargin = MENU_DISPLAYED_MARGIN;
                        } else {
                            oldMargin = MENU_DISPLAYED_MARGIN;
                            newMargin = -(v.getWidth() * (menuButtons.size() - 1));
                        }
                        a.setDuration(300); // in ms
                        topLevelMenu.startAnimation(a);
                    }
                }));

        menuButtons.values().forEach(buttonInfo -> {
            FloatingActionButton buttonView = createMenuButton(buttonInfo);
            buttonInfo.setView(buttonView);
            topLevelMenu.addView(buttonView);
        });

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE && data != null) {
            data.getData();
            Uri selectedImage = data.getData();
            loadImageFromUri(selectedImage);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        Log.d(TAG, "onTouchEvent: " + motionEvent);
        if (deformOngoing && motionEvent.getAction() == MotionEvent.ACTION_UP) {
            deformOngoing = false;
        }
        scaleGestureDetector.onTouchEvent(motionEvent);
        gestureDetector.onTouchEvent(motionEvent);
        if (!rotationDisabled) {
            rotationGestureDetector.onTouchEvent(motionEvent);
        }
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            customCanvas.setMScaleFactor(customCanvas.getMScaleFactor() * scaleGestureDetector.getScaleFactor());
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        int touchTolerance = 50;
        int cornerIndex = 0;

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if(e.getAction() == MotionEvent.ACTION_UP){
                return true;
            }
            return false;
        }
        @Override
        public boolean onScroll(@NonNull MotionEvent e1, @NonNull MotionEvent e2,
                                float distanceX, float distanceY) {

            if (deformMode) {
                float touchX;
                float touchY;
                float[] corners = customCanvas.getCornersInScreenSpace();
                if (deformOngoing) {
                    touchX = e2.getX();
                    touchY = e2.getY();
                    corners[2 * cornerIndex] = touchX;
                    corners[2 * cornerIndex + 1] = touchY;
                    customCanvas.setDeformation(corners);
                } else {
                    for (int i = 0; i < 4; i++) {
                        touchX = e1.getX();
                        touchY = e1.getY();
                        if (Math.abs(touchX - corners[2 * i]) < touchTolerance && (Math.abs(touchY - corners[2 * i + 1]) < touchTolerance)) {
                            deformOngoing = true;
                            cornerIndex = i;
                        }
                    }
                }
            }else {
                customCanvas.setPosition(customCanvas.getX() - distanceX, customCanvas.getY() - distanceY);
            }

            return true;
        }
    }

    private class RotationListener implements RotationGestureDetector.OnRotationGestureListener {

        long lastSeenEventId = 0L;
        float imageCurrentRotation = 0f;

        @Override
        public void OnRotation(RotationGestureDetector rotationDetector) {
            if (rotationDetector.getCurrentEventStartTime() != lastSeenEventId) {
                imageCurrentRotation = customCanvas.getRotation();
                lastSeenEventId = rotationDetector.getCurrentEventStartTime();
            }
            customCanvas.setRotation(imageCurrentRotation - rotationDetector.getAngle());
        }
    }

    FloatingActionButton createMenuButton(ButtonInfo buttonInfo) {
        FloatingActionButton newBtn = new FloatingActionButton(this);
        newBtn.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        newBtn.setAlpha(0.4f);
        newBtn.setImageResource(buttonInfo.getResId());
        newBtn.setOnClickListener(buttonInfo.getListener());
//        newBtn.set(buttonInfo.getLabel());

        return newBtn;
    }

    // Screen rotation destroys and recreates everything
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.

        savedInstanceState.putString("imageUri", imageUri);
        savedInstanceState.putBoolean("rotationDisabled", rotationDisabled);

        // etc.

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {

        super.onRestoreInstanceState(savedInstanceState);

        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.

        imageUri = savedInstanceState.getString("imageUri");
        if (imageUri != null) loadImageFromUri(Uri.parse(imageUri));
        rotationDisabled = savedInstanceState.getBoolean("rotationDisabled");
        Objects.requireNonNull(menuButtons.get(LOCK_ROTATION)).getView()
                .setBackgroundTintList(ColorStateList.valueOf(getColor(!rotationDisabled ? R.color.colorPrimary : R.color.colorAccent)));


    }

    private void loadImageFromUri(Uri imageUri) {
        if (imageUri != null) {
            this.imageUri = imageUri.toString();
            try {
                Bitmap selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                customCanvas.setBitmap(selectedImageBitmap);
                customCanvas.resetImageTransform();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
