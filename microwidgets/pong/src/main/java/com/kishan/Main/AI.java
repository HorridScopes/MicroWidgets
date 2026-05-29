package Main;
import java.awt.geom.Point2D;
import java.util.Random;

/**
 * AI controller for the right paddle opponent in Pong.
 * 
 * Implements a multi-layered AI system with:
 * - Layer 1: Reaction threshold detection using raycasting
 * - Layer 2: Ball trajectory prediction and movement interpolation
 * - Layer 3: Adaptive difficulty based on score differential
 * 
 * The AI uses physics-based raytracing to predict ball impact points and
 * adjusts prediction accuracy based on the score difference to provide
 * scalable difficulty levels.
 */
public class AI {
    private static final Random random = new Random();

    // Layer 1: Reaction threshold and raycasting
    /** X coordinate where AI begins reacting to the ball (screen center) */
    private static final double BASE_THRESHOLD_X = 150.0; // Screen center X
    /** Random range around threshold to add unpredictability (±20 pixels) */
    private static final double THRESHOLD_RANDOM_RANGE = 20.0; // ±20 pixels
    /** Current reaction threshold for this round */
    private static double currentThresholdX;

    // Layer 2: Prediction and movement
    /** Target Y position for the AI paddle center to move toward */
    private static double targetY = 150.0; // Target paddle center Y

    // Layer 3: Adaptive difficulty
    /** Maximum prediction error at easiest difficulty (±50 pixels) */
    private static final double MAX_PREDICTION_ERROR = 50.0; // ±50 pixels at easiest
    /** Minimum prediction error at hardest difficulty (±10 pixels) */
    private static final double MIN_PREDICTION_ERROR = 10.0; // ±10 pixels at hardest

    // State tracking
    /** Whether AI is currently reacting to the ball */
    private static boolean isReacting = false;
    /** Predicted impact point from ball trajectory raycast */
    private static Point2D.Double predictedImpact = null;

    /**
     * Main AI update method called every game tick.
     * 
     * Process:
     * 1. Calculates adaptive difficulty based on score difference
     * 2. Detects if ball has crossed the reaction threshold
     * 3. If so, performs raycast to predict ball impact point
     * 4. Moves paddle toward target with added error based on difficulty
     * 5. Resets reaction state when ball returns to center after scoring
     */
    public static void update() {
        // Layer 3: Calculate difficulty based on score differential
        int[] scores = Config.Scoreboard.GetScores();
        int playerScore = scores[Config.Scoreboard.LEFT]; // Human player
        int aiScore = scores[Config.Scoreboard.RIGHT]; // AI
        int scoreDiff = playerScore - aiScore;

        // Adaptive parameters
        double predictionError = Math.max(MIN_PREDICTION_ERROR,
                MAX_PREDICTION_ERROR - (scoreDiff * 5.0)); // Reduce error by 5 pixels per point difference

        // Layer 1: Check if ball crossed threshold
        double ballX = Config.Ball.location.x;
        if (!isReacting && ballX > currentThresholdX && Math.cos(Math.toRadians(Config.Ball.direction)) > 0) {
            // Ball crossed threshold - start reaction
            isReacting = true;

            // Perform raycast to predict impact
            predictedImpact = raycastBallTrajectory();
            if (predictedImpact != null) {
                // Layer 2: Calculate target position with randomness
                double idealY = predictedImpact.y;
                double error = (random.nextDouble() - 0.5) * 2 * predictionError; // ±predictionError
                targetY = idealY + error;

                // Clamp to paddle movement bounds
                double paddleHeight = Config.Paddle.RightPaddle.rectangle.getHeight();
                double minY = paddleHeight / 2;
                double maxY = Config.Paddle.screenArea.height - paddleHeight / 2;
                targetY = Math.max(minY, Math.min(maxY, targetY));

                System.out.println("AI: Ball crossed threshold at " + ballX + ", predicting impact at Y=" + idealY +
                        ", targeting Y=" + targetY + " (error: " + error + ")");
            }
        }

        // Layer 2: Move paddle toward target if reacting and delay passed
        if (isReacting) {
            moveTowardTarget();
        }
    }

    public static void reset() {
        isReacting = false;
        predictedImpact = new Point2D.Double(Config.Paddle.RightPaddle.getX(), Config.Paddle.screenArea.height / 2);
        targetY = Config.Paddle.screenArea.height / 2.0;
        currentThresholdX = BASE_THRESHOLD_X + (random.nextDouble() - 0.5) * 2 * THRESHOLD_RANDOM_RANGE;
        System.out.println("AI: Ball reset, new threshold: " + currentThresholdX);
    }

    /**
     * Raycasts the ball's trajectory to predict where it will impact the paddle's X
     * position.
     * 
     * Simulates ball physics forward in time, accounting for:
     * - Velocity components based on direction and speed
     * - Wall collisions (top/bottom boundaries)
     * - A small angle error to simulate imperfect prediction
     * 
     * @return Point2D with the predicted Y coordinate at the paddle's X position,
     *         or null if prediction fails
     */
    private static Point2D.Double raycastBallTrajectory() {
        // Simulate ball movement to predict where it hits paddle X
        double paddleX = Config.Paddle.RightPaddle.getX();

        // Current ball state
        double x = Config.Ball.location.x;
        double y = Config.Ball.location.y;
        double direction = Config.Ball.direction;
        double speed = Config.Ball.speed;

        // Add random angle deviation for imperfect prediction
        double angleError = (random.nextDouble() - 0.5) * 2 * 5.0; // ±5 degrees
        direction += angleError;

        int maxIterations = 1000; // Prevent infinite loops
        int iterations = 0;

        while (iterations < maxIterations) {
            // Calculate next position
            double rad = Math.toRadians(direction);
            double vx = speed * Math.cos(rad);
            double vy = speed * Math.sin(rad);

            double nextX = x + vx;
            double nextY = y + vy;

            // Check if we crossed paddle X
            if ((x <= paddleX && nextX >= paddleX) || (x >= paddleX && nextX <= paddleX)) {
                // Interpolate Y position at paddle X
                double t = (paddleX - x) / (nextX - x);
                double impactY = y + t * (nextY - y);
                return new Point2D.Double(paddleX, impactY);
            }

            // Check wall collisions (same as ball physics)
            if (nextY - Config.Ball.RADIUS <= 0 || nextY + Config.Ball.RADIUS >= Config.Paddle.screenArea.height) {
                // Reverse vertical direction
                direction = 360 - direction;
                direction %= 360;
                // Recalculate velocity with new direction
                continue;
            }

            // Update position
            x = nextX;
            y = nextY;
            iterations++;
        }

        // Fallback if raycast fails
        System.out.println("AI: Raycast failed after " + maxIterations + " iterations");
        return null;
    }

    /**
     * Moves the AI paddle toward the target Y position.
     * 
     * Controls paddle input based on distance to target:
     * - Moves up if target is above current position
     * - Moves down if target is below current position
     * - Stops movement if within dead zone (2 pixels)
     * 
     * Uses the same input system as the human player (input[0] for up, input[1] for
     * down).
     */
    private static void moveTowardTarget() {
        double currentCenterY = Config.Paddle.RightPaddle.rectangle.getCenterY();
        double distance = targetY - currentCenterY;

        // Dead zone - don't move if very close
        if (Math.abs(distance) < 2.0) {
            Config.Paddle.RightPaddle.input[0] = false;
            Config.Paddle.RightPaddle.input[1] = false;
            isReacting = false;
            return;
        }

        // Move toward target
        if (distance > 0) {
            // Move down
            Config.Paddle.RightPaddle.input[0] = false;
            Config.Paddle.RightPaddle.input[1] = true;
        } else {
            // Move up
            Config.Paddle.RightPaddle.input[0] = true;
            Config.Paddle.RightPaddle.input[1] = false;
        }
    }

    // Initialize AI
    static {
        currentThresholdX = BASE_THRESHOLD_X + (random.nextDouble() - 0.5) * 2 * THRESHOLD_RANDOM_RANGE;
    }
}
