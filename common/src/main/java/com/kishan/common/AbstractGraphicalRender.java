package com.kishan.common;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

public abstract class AbstractGraphicalRender {

    protected RenderContext writeToContext;

    public void setRenderContext(RenderContext writeToContext) {
        this.writeToContext = writeToContext;
    }

    protected abstract Dimension getScreenSize();

    protected abstract BufferedImage buildImage(Dimension screenSize);

    public void postImage() {
        BufferedImage img = buildImage(getScreenSize());
        writeToContext.setRenderImage(img);
    }
}
