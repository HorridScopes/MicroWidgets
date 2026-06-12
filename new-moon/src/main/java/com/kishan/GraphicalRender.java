package com.kishan;

import com.kishan.common.AbstractGraphicalRender;
import com.kishan.common.RenderContext;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * GraphicalRender class responsible for rendering game visuals.
 * 
 * Manages:
 * - Building the frame image with all game objects
 * - Drawing paddles, ball, and background
 * - Sending rendered images to the screen for display
 */
public class GraphicalRender extends AbstractGraphicalRender {

        public GraphicalRender(RenderContext renderContext) {
                setRenderContext(renderContext);
        }

        /**
         * Builds a complete frame image containing all game elements.
         * 
         * Rendering order:
         * 1. Fills background with background color
         * 
         * @param screenSize The dimensions of the screen to render to
         * @return BufferedImage containing the rendered frame
         */
        @Override
        protected BufferedImage buildImage(Dimension screenSize) {
                BufferedImage bIm = new BufferedImage(screenSize.width, screenSize.height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = bIm.createGraphics();
                g2d.setColor(Config.Rendering.BA_COLOR);
                g2d.fillRect(0, 0, screenSize.width, screenSize.height);

                g2d.dispose();
                return bIm;
        }

        @Override
        protected Dimension getScreenSize() {
                return writeToContext.getContentSize();
        }

}