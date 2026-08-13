package com.example.password_managerui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class QRScannerOverlay extends View {

    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final float scanSize;

    public QRScannerOverlay(Context context) {
        this(context, null);
    }

    public QRScannerOverlay(
            Context context,
            AttributeSet attrs
    ) {
        super(context, attrs);

        scanSize =
                getResources()
                        .getDisplayMetrics()
                        .density * 270f;

        overlayPaint.setColor(0xFF000000);

        clearPaint.setXfermode(
                new android.graphics.PorterDuffXfermode(
                        PorterDuff.Mode.CLEAR
                )
        );

        setLayerType(
                View.LAYER_TYPE_SOFTWARE,
                null
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        float left =
                centerX - scanSize / 2f;

        float top =
                centerY - scanSize / 2f;

        float right =
                centerX + scanSize / 2f;

        float bottom =
                centerY + scanSize / 2f;

        RectF scanRect =
                new RectF(
                        left,
                        top,
                        right,
                        bottom
                );

        // Black everything
        canvas.drawRect(
                0,
                0,
                getWidth(),
                getHeight(),
                overlayPaint
        );

        // Make scanner square transparent
        canvas.drawRoundRect(
                scanRect,
                18f,
                18f,
                clearPaint
        );
    }
}