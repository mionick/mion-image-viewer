package io.mionick.imageviewer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import io.mionick.imageviewer.gestures.RotationGestureDetector;

public class MainActivity extends AppCompatActivity {

    private static final int MENU_DISPLAYED_MARGIN = 0;
    private static final int MENU_HIDDEN_MARGIN = -800;

    public static final int PICK_IMAGE = 1;

    private String imageUri;
    private ImageView image;

    // Scale variables
    private ScaleGestureDetector scaleGestureDetector;

    private float mScaleFactor = 1.0f;

    private GestureDetector gestureDetector;
    private RotationGestureDetector rotationGestureDetector;

    private ImageView rotationLockedIcon;
    private boolean rotationEnabled = true;
    private boolean menuVisible = true;

    private ViewGroup menu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        menu = findViewById(R.id.menu);

        FloatingActionButton btnHideShow = findViewById(R.id.btn_hide_show);
        btnHideShow.setOnClickListener(new View.OnClickListener() {

            int newMargin = 0;
            int oldMargin = 0;

            Animation a = new Animation() {

                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) menu.getLayoutParams();
                    params.setMargins(oldMargin + (int)((newMargin - oldMargin) * interpolatedTime),0,0,0);
                    menu.setLayoutParams(params);
                }
            };

            @Override
            public void onClick(View v) {

                menuVisible = !menuVisible;
                ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) menu.getLayoutParams();
                if (menuVisible) {
                    //p.setMargins(0,0,0,0);
                    oldMargin = MENU_HIDDEN_MARGIN;
                    newMargin = MENU_DISPLAYED_MARGIN;
                } else {
                    //p.setMargins(0,0,0,-50);
                    oldMargin = MENU_DISPLAYED_MARGIN;
                    newMargin = MENU_HIDDEN_MARGIN;
                }
                a.setDuration(300); // in ms
                menu.startAnimation(a);
            }
        });

        FloatingActionButton getImageButton = findViewById(R.id.get_image);
        getImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
            }
        });

        FloatingActionButton resetButton = findViewById(R.id.reset);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mScaleFactor  = 1.0f;
                image.setScaleX(mScaleFactor);
                image.setScaleY(mScaleFactor);

                image.setX(0);
                image.setY(0);

                image.setRotation(0);
            }
        });

        FloatingActionButton lockRotationButton = findViewById(R.id.btn_lock_rotation);
        rotationLockedIcon = findViewById(R.id.icon_rotation_locked);
        lockRotationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rotationEnabled  = !rotationEnabled;
                rotationLockedIcon.setVisibility(rotationEnabled ? View.INVISIBLE : View.VISIBLE);
            }
        });

        image = findViewById(R.id.image);

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
        gestureDetector = new GestureDetector(this, new GestureListener());
        rotationGestureDetector = new RotationGestureDetector(new RotationListener());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE && data != null) {
            data.getData();
            Uri selectedImage = data.getData();
            image.setImageURI(selectedImage);
            if (selectedImage != null) {
                imageUri = selectedImage.toString();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        scaleGestureDetector.onTouchEvent(motionEvent);
        gestureDetector.onTouchEvent(motionEvent);
        if (rotationEnabled) {
            rotationGestureDetector.onTouchEvent(motionEvent);
        }
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector){
            mScaleFactor *= scaleGestureDetector.getScaleFactor();
            image.setScaleX(mScaleFactor);
            image.setScaleY(mScaleFactor);
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            image.setX(image.getX() - distanceX);
            image.setY(image.getY() - distanceY);
            return true;
        }
    }

    private class RotationListener implements RotationGestureDetector.OnRotationGestureListener {

        long lastSeenEventId = 0L;
        float imageCurrentRotation = 0f;

        @Override
        public void OnRotation(RotationGestureDetector rotationDetector) {
            if (rotationDetector.getCurrentEventStartTime() != lastSeenEventId) {
                imageCurrentRotation = image.getRotation();
                lastSeenEventId = rotationDetector.getCurrentEventStartTime();
            }
            image.setRotation(imageCurrentRotation-rotationDetector.getAngle());
        }
    }




// Screen rotation
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.

        savedInstanceState.putString("imageUri", imageUri);
        savedInstanceState.putBoolean("rotationEnabled", rotationEnabled);

        // etc.

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {

        super.onRestoreInstanceState(savedInstanceState);

        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.

        imageUri = savedInstanceState.getString("imageUri");
        image.setImageURI(Uri.parse(imageUri));
        rotationEnabled = savedInstanceState.getBoolean("rotationEnabled");
        rotationLockedIcon.setVisibility(rotationEnabled ? View.INVISIBLE : View.VISIBLE);


    }
}
