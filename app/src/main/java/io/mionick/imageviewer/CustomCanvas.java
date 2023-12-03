package io.mionick.imageviewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

import lombok.Getter;

@Getter
public class CustomCanvas extends View {
    private final Matrix matrix;
    private Bitmap bitmap;

    private float x = 0, y = 0, w, h, rotation = 0;
    private float mScaleFactor = 1.0f;
    private float[] polyToPoly = new float[8];
    private boolean deformHandlesVisible;
    private boolean gridVisible;
    private Paint paint = new Paint();
    float r = 10;
    int numberOfDotsPerLine = 4;


    public CustomCanvas(Context context) {
        super(context);
        matrix = new Matrix();
        paint.setColor(Color.MAGENTA);
    }

    protected void onDraw(Canvas canvas) {
        if (bitmap != null) {
            canvas.setMatrix(matrix);
            canvas.drawBitmap(bitmap, 0, 0, null);
            canvas.setMatrix(null);

            if (gridVisible) {
                float[] gridCoords = getGridCoords();
                matrix.mapPoints(gridCoords);
                paint.setColor(Color.MAGENTA);
                for (int i = 0; i < gridCoords.length/2; i++) {
                    float x = gridCoords[2 * i];
                    float y = gridCoords[2 * i + 1];
                    canvas.drawCircle(x, y, r, paint);
                }
            }

            if (deformHandlesVisible) {
                float[] handles = getCornersInScreenSpace();
                paint.setColor(Color.RED);
                for (int i = 0; i < 4; i++) {
                    float x = handles[2 * i];
                    float y = handles[2 * i + 1];
                    canvas.drawCircle(x, y, r, paint);
                }
            }

        }
    }

    public float[] getCornersInScreenSpace() {
        float[] corners = getUnTransformedCorners();
        matrix.mapPoints(corners);
        return corners;
    }

    private float[] getUnTransformedCorners() {
        float x = 0, y = 0;
        return new float[]{x, y,
                x + w, y,
                x, y + h,
                x + w, y + h
        };
    }

    public void resetImageTransform() {
        rotation = 0;
        polyToPoly = getUnTransformedCorners();


        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.getContext().getSystemService(WindowManager.class)
                .getDefaultDisplay()
                .getMetrics(displayMetrics);

        float hs = displayMetrics.heightPixels;
        float ws = displayMetrics.widthPixels;
        float wi = bitmap.getWidth();
        float hi = bitmap.getHeight();
        float rs = ws / hs;
        float ri = wi / hi;

        mScaleFactor = rs > ri ? hs / hi : ws / wi;


        y = (int) ((hs - hi) / 2);
        x = (int) ((ws - wi) / 2);

        refreshMatrixTransform();

        this.invalidate();
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
        refreshMatrixTransform();
        this.invalidate();
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        refreshMatrixTransform();
        this.invalidate();
    }

    public void setMScaleFactor(float mScaleFactor) {
        this.mScaleFactor = mScaleFactor;
        this.invalidate();
    }

    public synchronized void setDeformation(float[] newCornerPositionsTransformed) {
        polyToPoly = newCornerPositionsTransformed;
        matrix.reset();
        matrix.setPolyToPoly(getUnTransformedCorners(), 0, newCornerPositionsTransformed, 0, 4);
        rotation = 0;
        mScaleFactor = 1;
        x = 0;
        y = 0;
        this.invalidate();
    }


    public void setDeformHandlesVisible(boolean deformMode) {
        this.deformHandlesVisible = deformMode;
        this.invalidate();
    }

    public void setGridVisible(boolean gridVisible) {
        this.gridVisible = gridVisible;
        this.invalidate();
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        this.w = bitmap.getWidth();
        this.h = bitmap.getHeight();

        this.resetImageTransform();
        refreshMatrixTransform();
        this.invalidate();
    }

    private void refreshMatrixTransform() {
        matrix.reset();
        matrix.setPolyToPoly(getUnTransformedCorners(), 0, polyToPoly, 0, 4);
        matrix.postRotate(rotation, w / 2, h / 2);
        matrix.postScale(mScaleFactor, mScaleFactor, w / 2, h / 2);
        matrix.postTranslate(x, y);
    }

    private float[] getGridCoords() {
        float spacing = w / (numberOfDotsPerLine - 1f);
        int lines = 1 + (int) (h / spacing);
        float[] gridCoords = new float[2 * numberOfDotsPerLine * lines];
        for (int i = 0; i < gridCoords.length / 2; i ++) {
            gridCoords[2 * i] = (i % numberOfDotsPerLine) * spacing;
            //noinspection IntegerDivisionInFloatingPointContext, intentional to get which row we are rendering
            gridCoords[2 * i + 1] = (i / numberOfDotsPerLine) * spacing;
        }
        return gridCoords;
    }

}
