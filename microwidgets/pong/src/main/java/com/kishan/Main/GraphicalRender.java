package Main;
import java.awt.Dimension;
import java.awt.Font;
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
public class GraphicalRender {

        /** Reference to the screen/rendering context to draw to */
        public static Screen writeToContext;

        /**
         * Builds a complete frame image containing all game elements.
         * 
         * Rendering order:
         * 1. Fills background with background color
         * 2. Renders left paddle
         * 3. Renders right paddle
         * 4. Renders ball
         * 
         * @param screenSize The dimensions of the screen to render to
         * @return BufferedImage containing the rendered frame
         */
        public BufferedImage buildImage(Dimension screenSize) {
                BufferedImage bIm = new BufferedImage(screenSize.width, screenSize.height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = bIm.createGraphics();
                g2d.setColor(Config.Rendering.BA_COLOR);
                g2d.fillRect(0, 0, screenSize.width, screenSize.height);

                RenderObject leftPaddleRender = Config.Paddle.LeftPaddle.getDraw();
                RenderObject rightPaddleRender = Config.Paddle.RightPaddle.getDraw();
                g2d.setColor(leftPaddleRender.color);
                g2d.fill(leftPaddleRender.shape);
                g2d.fill(rightPaddleRender.shape);
                g2d.setColor(Config.Rendering.SCOREBOARD_COLOR);
                Font leftfont = Config.deriveFontSize(
                                Config.Rendering.ScoreboardFont,
                                Config.Scoreboard.GetScores()[0] + "",
                                20);

                g2d.setFont(leftfont);
                g2d.drawString(
                                Config.Scoreboard.GetScores()[0] + "",
                                (int) Config.getCenteredTextPosition(
                                                leftfont,
                                                Config.Scoreboard.GetScores()[0] + "",
                                                Config.Paddle.screenArea.width * 0.3,
                                                0).x,
                                50);

                Font rightfont = Config.deriveFontSize(
                                Config.Rendering.ScoreboardFont,
                                Config.Scoreboard.GetScores()[1] + "",
                                20);

                g2d.setFont(rightfont);
                g2d.drawString(
                                Config.Scoreboard.GetScores()[1] + "",
                                (int) Config.getCenteredTextPosition(
                                                rightfont,
                                                Config.Scoreboard.GetScores()[1] + "",
                                                Config.Paddle.screenArea.width * 0.7,
                                                0).x,
                                50);
                RenderObject ballRender = Config.Ball.getDraw();
                g2d.setColor(ballRender.color);
                g2d.fill(ballRender.shape);

                g2d.dispose();
                return bIm;
        }

        /**
         * Builds and sends the current frame to the screen for rendering.
         * 
         * Constructs a new image of the current game state and passes it to
         * the screen context to be displayed on the next paint operation.
         */
        public void postImage() {
                BufferedImage img = buildImage(writeToContext.getContentSize());
                writeToContext.setRenderImage(img);
        }

}