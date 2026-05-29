package Main;
import java.awt.geom.Point2D;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
// import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.awt.geom.Ellipse2D;
import java.awt.event.KeyEvent;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;

public class Config {
    /**
     * Abstract base class for paddle entities in the game.
     * 
     * Manages:
     * - Paddle position and dimensions
     * - Movement within screen bounds
     * - Input tracking (key states)
     * - Rendering data
     * - Static paddle instances (LeftPaddle, RightPaddle)
     */
    public static abstract class Paddle {
        /** Input state for paddle: [0] = up, [1] = down */
        public boolean[] input = new boolean[2];
        /** Distance from left/right edge of screen where paddle should be positioned */
        protected static float distanceFromWall;
        /** Screen dimensions for boundary checking */
        protected static Dimension screenArea;
        /** Paddle movement speed in pixels per frame */
        private static float baseMovementSpeed = 2;
        private static float DynamicMovementSpeed = 2;
        private static float DynamicSpeedModifier = 0.05f;
        /** Rectangle representing paddle position and dimensions */
        protected Rectangle2D.Double rectangle;
        /** Left player's paddle instance */
        public static Paddle LeftPaddle;
        /** Right player's paddle instance (AI) */
        public static Paddle RightPaddle;

        /**
         * Constructs a paddle with given dimensions.
         * 
         * @param width  The paddle width in pixels
         * @param height The paddle height in pixels
         */
        public Paddle(double width, double height) {
            rectangle = new Rectangle2D.Double(0, 0, width, height);
        }

        /**
         * Moves the paddle vertically within screen bounds.
         * 
         * @param input Movement amount: negative for up, positive for down, 0 for no
         *              movement
         */
        public void Move(double input) {
            rectangle = Config.TranslateRectangle(rectangle, 0, input * DynamicMovementSpeed);
            while (rectangle.getMaxY() > screenArea.height) {
                rectangle = Config.TranslateRectangle(rectangle, 0, -1);
            }
            while (rectangle.getMinY() < 0) {
                rectangle = Config.TranslateRectangle(rectangle, 0, 1);
            }
        }

        public static void calculateDynamicSpeed() {
            DynamicMovementSpeed = baseMovementSpeed + (DynamicSpeedModifier * Ball.paddleHitsDuringRound);
        }

        /**
         * Resets paddle to center of the screen vertically.
         */
        public void resetToCenter() {
            rectangle = Config.setRectanglePosition(rectangle, rectangle.x,
                    (screenArea.getHeight() - rectangle.getHeight()) / 2);
            DynamicMovementSpeed = baseMovementSpeed;
        }

        /**
         * Gets the X coordinate of this paddle.
         * Implemented by subclasses to position left/right paddles.
         * 
         * @return The X coordinate of the paddle
         */
        protected abstract double getX();

        /**
         * Sets the play area bounds used for paddle constraints.
         * 
         * @param dimension The screen dimensions
         */
        public void setPlayBounds(Dimension dimension) {
            screenArea = dimension;
        }

        /**
         * Sets the distance from wall where paddle should be positioned.
         * 
         * @param distance Distance in pixels from the edge
         */
        public static void setDistanceFromWall(float distance) {
            distanceFromWall = distance;
        }

        /**
         * Gets the render object for this paddle.
         * 
         * @return A RenderObject containing the paddle's shape and color
         */
        public RenderObject getDraw() {
            return new RenderObject(rectangle, RenderObject.PADDLE_COLOR);
        }

        /**
         * Converts input state array to a numeric movement direction.
         * 
         * @param inputs Boolean array where [0] = up, [1] = down
         * @return -1 for up, 1 for down, 0 for no movement or conflicting inputs
         */
        public static int organizeInput(boolean[] inputs) {
            if (inputs[0] && !inputs[1]) {
                return -1;
            } else if (inputs[1] && !inputs[0]) {
                return 1;
            }
            return 0;
        }

    }

    /**
     * Left paddle class - positioned on the left side of the screen.
     * Extends Paddle and is typically controlled by the human player.
     */
    public class Left extends Paddle {

        /**
         * Constructs a left paddle with given dimensions.
         * 
         * @param width  The paddle width in pixels
         * @param height The paddle height in pixels
         */
        public Left(double width, double height) {
            super(width, height);
        }

        /**
         * Gets the X coordinate for the left paddle (near left wall).
         * 
         * @return X position based on distanceFromWall
         */
        @Override
        protected double getX() {
            return distanceFromWall;
        }

        /**
         * Gets the render object for the left paddle positioned at the left wall.
         * 
         * @return RenderObject containing paddle shape and color
         */
        @Override
        public RenderObject getDraw() {
            Rectangle2D renderObject = new Rectangle2D.Double(getX(), rectangle.getMinY(),
                    rectangle.getWidth(), rectangle.getHeight());
            return new RenderObject(renderObject, RenderObject.PADDLE_COLOR);
        }
    }

    /**
     * Right paddle class - positioned on the right side of the screen.
     * Extends Paddle and is typically controlled by the AI opponent.
     */
    public class Right extends Paddle {

        /**
         * Constructs a right paddle with given dimensions.
         * 
         * @param width  The paddle width in pixels
         * @param height The paddle height in pixels
         */
        public Right(double width, double height) {
            super(width, height);
        }

        /**
         * Gets the X coordinate for the right paddle (near right wall).
         * 
         * @return X position calculated from screen width and distanceFromWall
         */
        @Override
        protected double getX() {
            return screenArea.width - distanceFromWall - rectangle.getWidth();
        }

        /**
         * Gets the render object for the right paddle positioned at the right wall.
         * 
         * @return RenderObject containing paddle shape and color
         */
        @Override
        public RenderObject getDraw() {
            Rectangle2D renderObject = new Rectangle2D.Double(getX(), rectangle.getMinY(),
                    rectangle.getWidth(), rectangle.getHeight());
            return new RenderObject(renderObject, RenderObject.PADDLE_COLOR);
        }
    }

    /**
     * Ball class managing the pong ball's physics and behavior.
     * 
     * Handles:
     * - Position and movement with velocity components
     * - Direction in degrees (0=right, 90=up, 180=left, 270=down)
     * - Collision detection with paddles and walls
     * - Speed increase on paddle hits
     * - Scoring detection
     */
    public static class Ball {
        /** Ball radius in pixels */
        static final int RADIUS = 10;
        /** Number of paddle hits in current round (used for speed increase) */
        static int paddleHitsDuringRound = 0;
        /** Current position of the ball */
        static Point2D.Double location;
        /** Direction in degrees: 0=right, 90=up, 180=left, 270=down */
        static double direction;
        /** Base speed (pixels per frame) at start of round */
        static double baseSpeed = 3;
        /** Speed increase per paddle hit */
        static float speedIncrease = 0.03f;
        /** Current speed (pixels per frame) */
        static double speed = 3;
        /** Scaling factor for paddle hit angle adjustment */
        static final double ANGLE_SCALE = 1;

        /**
         * Initializes the ball at the center of the screen.
         * Called at game start and after each scoring.
         */
        public static void init() {
            location = new Point2D.Double(Paddle.screenArea.width / 2.0, Paddle.screenArea.height / 2.0);
            direction = 180;
        }

        /**
         * Updates ball position and handles all physics simulation per frame.
         * 
         * Process:
         * 1. Calculates velocity from direction and speed
         * 2. Updates position
         * 3. Detects and handles wall collisions (top/bottom)
         * 4. Detects and handles paddle collisions
         * 5. Detects scoring (off-screen left/right)
         */
        public static void Move() {
            // Calculate velocity components
            double rad = Math.toRadians(direction);
            double vx = speed * Math.cos(rad);
            double vy = speed * Math.sin(rad);

            // Update position
            location.x += vx;
            location.y += vy;

            // Check wall collisions (floor and ceiling)
            if (location.y - RADIUS <= 0 || location.y + RADIUS >= Paddle.screenArea.height) {
                // Reverse vertical direction
                direction = 360 - direction;
                direction %= 360; // normalize to 0-360
            }

            // Check paddle collisions
            Ellipse2D ballShape = new Ellipse2D.Double(location.x - RADIUS, location.y - RADIUS, RADIUS * 2,
                    RADIUS * 2);

            // Left paddle collision
            if (Paddle.LeftPaddle != null) {
                Rectangle2D leftRect = new Rectangle2D.Double(Paddle.LeftPaddle.getX(),
                        Paddle.LeftPaddle.rectangle.getMinY(),
                        Paddle.LeftPaddle.rectangle.getWidth(), Paddle.LeftPaddle.rectangle.getHeight());
                if (ballShape.intersects(leftRect)) {
                    handlePaddleCollision(Paddle.LeftPaddle);
                }
            }

            // Right paddle collision
            if (Paddle.RightPaddle != null) {
                Rectangle2D rightRect = new Rectangle2D.Double(Paddle.RightPaddle.getX(),
                        Paddle.RightPaddle.rectangle.getMinY(),
                        Paddle.RightPaddle.rectangle.getWidth(), Paddle.RightPaddle.rectangle.getHeight());
                if (ballShape.intersects(rightRect)) {
                    handlePaddleCollision(Paddle.RightPaddle);
                }
            }

            // Check for off-screen scoring
            if (location.x - RADIUS < 0) {
                // Ball exited left side - right player scores
                Scoreboard.IncrementScore(Scoreboard.RIGHT);
                Main.triggerScoringSequence();
            } else if (location.x + RADIUS > Paddle.screenArea.width) {
                // Ball exited right side - left player scores
                Scoreboard.IncrementScore(Scoreboard.LEFT);
                Main.triggerScoringSequence();
            }
        }

        /**
         * Handles collision between ball and paddle.
         * 
         * Updates ball direction based on paddle hit location,
         * increases ball speed, and repositions ball to prevent clipping.
         * 
         * @param paddle The paddle that was hit
         */
        private static void handlePaddleCollision(Paddle paddle) {
            System.out.println("Ball collision with paddle!");
            // Calculate angle adjustment based on hit position
            paddleHitsDuringRound++;
            speed += paddleHitsDuringRound * speedIncrease;
            double paddleCenterY = paddle.rectangle.getCenterY();
            double ballCenterY = location.y;
            double offset = ballCenterY - paddleCenterY;
            double angleAdjustment = offset * ANGLE_SCALE;

            // Add 180 degrees first, then the adjustment
            if (paddle == Config.Paddle.LeftPaddle) {
                direction = angleAdjustment;
                do {
                    location.x += 1;
                } while (location.x + RADIUS < Config.Paddle.LeftPaddle.rectangle.getMaxX());
            } else if (paddle == Config.Paddle.RightPaddle) {
                direction = 180 - angleAdjustment;
                do {
                    location.x -= 1;
                } while (location.x + RADIUS > Config.Paddle.RightPaddle.getX());
            }
            direction %= 360; // normalize to 0-360

        }

        /**
         * Resets ball to center position and direction.
         * 
         * Direction depends on who last scored:
         * - If left scored: ball moves right (0°)
         * - If right scored: ball moves left (180°)
         */
        public static void resetToCenter() {
            location.x = Paddle.screenArea.width / 2.0;
            location.y = Paddle.screenArea.height / 2.0;
            if (Scoreboard.LastScored() == Scoreboard.LEFT) {
                direction = 0;
            } else {
                direction = 180; // Ensure that when a player scores, then the opposite player serves, with the
                                 // user always serving at the start of a 0-0 game
            }
            speed = baseSpeed;
        }

        /**
         * Gets the render object for the ball.
         * 
         * @return RenderObject containing ball ellipse and white color
         */
        public static RenderObject getDraw() {
            Ellipse2D shape = new Ellipse2D.Double(location.x - RADIUS, location.y - RADIUS, RADIUS * 2, RADIUS * 2);
            return new RenderObject(shape, RenderObject.BALL_COLOR);
        }
    }

    /**
     * Scoreboard class for managing game scores.
     * 
     * Tracks:
     * - Current scores for left and right players
     * - Which player scored last
     * - Provides score retrieval and reset functionality
     */
    public static class Scoreboard {
        /** Constant for left player index */
        public static final int LEFT = 0;
        /** Constant for right player index */
        public static final int RIGHT = 1;
        /** Array holding scores: [0] = left, [1] = right */
        private static int[] scores = new int[2];
        /** Tracks which player scored last */
        private static int lastScored = -1;

        /**
         * Increments the score for the specified player.
         * 
         * @param player Either LEFT or RIGHT constant
         */
        public static void IncrementScore(int player) {
            if (player == LEFT || player == RIGHT) {
                scores[player]++;
                lastScored = player;
                System.out.println("SCORE: Player " + (player == LEFT ? "LEFT" : "RIGHT") + " scores! Score: "
                        + scores[LEFT] + "-" + scores[RIGHT]);
            }
        }

        /**
         * Gets a copy of the current scores.
         * 
         * @return Array with scores: [0] = left, [1] = right
         */
        public static int[] GetScores() {
            return scores.clone();
        }

        /**
         * Gets which player scored last.
         * 
         * @return LEFT, RIGHT, or -1 if no one has scored yet
         */
        public static int LastScored() {
            return lastScored;
        }

        /**
         * Resets both scores to 0 and clears last scorer tracking.
         */
        public static void Reset() {
            scores[0] = 0;
            scores[1] = 0;
            lastScored = -1;
        }
    }

    /**
     * Rendering configuration class containing visual constants.
     * 
     * Manages:
     * - Background color
     * - Game fonts loaded from resources
     */
    public static class Rendering {
        /** Background color (dark gray) */
        public static final Color BA_COLOR = new Color(30, 30, 30);
        public static final Color SCOREBOARD_COLOR = new Color(150, 150, 150);
        /** Font for displaying scoreboard text */
        public static final Font ScoreboardFont;

        static {
            System.out.println("Loading font resource for scoreboard...");
            try (InputStream fontStream = Config.class.getResourceAsStream("/Main/Resources/MedodicaRegular.otf")) {
                if (fontStream == null) {
                    throw new IllegalStateException("Font resource not found");
                }
                ScoreboardFont = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(24f);
            } catch (FontFormatException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Input configuration class for keyboard controls.
     * 
     * Stores key bindings for paddle control.
     */
    public static class Input {
        /** Key code for left paddle up movement */
        public static int LeftPaddleUp = KeyEvent.VK_W;
        /** Key code for left paddle down movement */
        public static int LeftPaddleDown = KeyEvent.VK_S;
    }

    /**
     * Translates a rectangle by the given delta values.
     * 
     * @param rect   The rectangle to translate
     * @param deltaX Change in X coordinate
     * @param deltaY Change in Y coordinate
     * @return New translated rectangle
     */
    public static Rectangle2D.Double TranslateRectangle(Rectangle2D.Double rect, double deltaX, double deltaY) {
        return new Rectangle2D.Double(rect.getX() + deltaX, rect.getY() + deltaY, rect.getWidth(), rect.getHeight());
    }

    /**
     * Sets a rectangle to a new position while preserving dimensions.
     * 
     * @param rect The rectangle to reposition
     * @param newX New X coordinate
     * @param newY New Y coordinate
     * @return New rectangle at the specified position
     */
    public static Rectangle2D.Double setRectanglePosition(Rectangle2D.Double rect, double newX, double newY) {
        return new Rectangle2D.Double(newX, newY, rect.getWidth(), rect.getHeight());
    }

    /**
     * Derives the optimal font size to fit a string within a maximum width.
     * Uses binary search to find the largest font size that keeps the text within
     * bounds.
     *
     * @param baseFont the base font to derive sizes from
     * @param text     the string to measure
     * @param maxWidth the maximum width constraint in pixels
     * @return the optimal font size
     */
    public static Font deriveFontSize(Font baseFont, String text, double width) {
        float size = 1f;
        Font newFont = baseFont.deriveFont(size);

        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();

        while (g2d.getFontMetrics(newFont).stringWidth(text) < width) {
            size++;
            newFont = newFont.deriveFont(size);
        }

        g2d.dispose();
        return newFont;
    }

    /**
     * Calculates the position to render text so it is centered at a given point.
     * Takes into account the font metrics to properly center vertically and
     * horizontally.
     *
     * @param font    the font to use for measurement
     * @param text    the string to position
     * @param centerX the desired center X coordinate
     * @param centerY the desired center Y coordinate
     * @return a Point2D with the x, y coordinates where the text should be rendered
     */
    public static Point2D.Double getCenteredTextPosition(Font font, String text, double centerX, double centerY) {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();

        FontMetrics fm = g2d.getFontMetrics(font);
        int textWidth = fm.stringWidth(text);
        int ascent = fm.getAscent();
        int descent = fm.getDescent();

        double x = centerX - (textWidth / 2.0);
        double y = centerY + (ascent / 2.0) - (descent / 2.0);

        g2d.dispose();
        return new Point2D.Double(x, y);
    }

}
