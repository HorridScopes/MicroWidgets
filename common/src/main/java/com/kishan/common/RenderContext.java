package com.kishan.common;

import java.awt.Dimension;
import java.awt.Image;

public interface RenderContext {

    Dimension getContentSize();

    void setRenderImage(Image img);
}
