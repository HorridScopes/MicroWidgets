package com.kishan.common;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.SwingUtilities;

public abstract class AbstractGraphicalRender {

    protected RenderContext writeToContext;

    private static final ExecutorService IMAGE_BUILDER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "mw-image-builder");
        t.setDaemon(true);
        return t;
    });

    public void setRenderContext(RenderContext writeToContext) {
        this.writeToContext = writeToContext;
    }

    protected abstract Dimension getScreenSize();

    protected abstract BufferedImage buildImage(Dimension screenSize);

    /**
     * Offload image building to a background thread then post result on EDT.
     */
    public void postImage() {
        final Dimension size = getScreenSize();
        IMAGE_BUILDER.submit(() -> {
            try {
                final BufferedImage img = buildImage(size);
                SwingUtilities.invokeLater(() -> {
                    if (writeToContext != null) {
                        writeToContext.setRenderImage(img);
                    }
                });
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }
}
