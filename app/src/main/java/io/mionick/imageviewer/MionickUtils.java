package io.mionick.imageviewer;

import android.graphics.Point;
import android.graphics.PointF;
import android.util.Pair;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class MionickUtils {
    public void iteratePoints(float[] points, BiConsumer<PointF, Integer> func) {
        for (int i = 0; i < 4; i++) {
            func.accept(new PointF(points[2 * i], points[2 * i + 1]), i);
        }
    }
}
