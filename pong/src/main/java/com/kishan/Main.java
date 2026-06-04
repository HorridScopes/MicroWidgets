
package com.kishan;

import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import com.kishan.common.IThread;
import javax.swing.SwingUtilities;

/**
 * Main class that initializes and controls the Pong game.
 * 
 * Responsible for:
 * - Setting up the game window and rendering context
 * - Initializing paddles, ball, and AI
 * - Managing the update and render loops
 * - Handling keyboard input for the left paddle
 * - Controlling game state during scoring sequences
 * 
 * The game runs at 60 FPS for both rendering and game logic updates.
 */
public class Main {

    /** Graphics rendering engine */
    protected static GraphicalRender graphicalRender;
    /** Left player's paddle */
    public static Config.Paddle leftPaddle;
    /** Right player's paddle (AI controlled) */
    public static Config.Paddle rightPaddle;

    /**
     * Entry point for the Pong game.
     * 
     * Initializes the game window, sets up all game entities (paddles, ball),
     * starts the rendering and update loops, and attaches keyboard listeners.
     * 
     * @param args Command line arguments (not used)
     * @throws Exception If initialization fails
     */
    public static void main(String[] args) throws Exception {
        Screen screen = new Screen(new Dimension(300, 300), true, false, true);

        graphicalRender = new GraphicalRender(screen);
        Config.Paddle.screenArea = screen.getContentSize();
        Config.Paddle.setDistanceFromWall(10);
        Config.Paddle.LeftPaddle = new Config().new Left(10, 60);
        Config.Paddle.RightPaddle = new Config().new Right(10, 60);

        Config.Paddle.LeftPaddle.resetToCenter();
        Config.Paddle.RightPaddle.resetToCenter();
        AI.reset();

        Config.Ball.init();

        screen.startRenderLoop(1000 / 60);
        screen.setVisible(true);
        updateThread.startThread(1000 / 60);

        screen.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == Config.Input.LeftPaddleUp) {
                    Config.Paddle.LeftPaddle.input[0] = true;
                }
                if (e.getKeyCode() == Config.Input.LeftPaddleDown) {
                    Config.Paddle.LeftPaddle.input[1] = true;
                }
            }

            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == Config.Input.LeftPaddleUp) {
                    Config.Paddle.LeftPaddle.input[0] = false;
                }
                if (e.getKeyCode() == Config.Input.LeftPaddleDown) {
                    Config.Paddle.LeftPaddle.input[1] = false;
                }
            }
        });

        screen.addWindowFocusListener(new WindowFocusListener() {

            @Override
            public void windowGainedFocus(WindowEvent e) {
                try {
                    Thread.sleep(1000);
                    System.out.println("Window gained focus, resuming update loop");
                    isPaused = false;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                System.out.println("Window lost focus, pausing update loop");
                isPaused = true;
            }

        });

    }

    /**
     * Tracks whether the game is currently paused (e.g., during score display, or
     * inactive screen)
     */
    private static boolean isPaused = true;
    static final IThread updateThread = IThread.create(() -> SwingUtilities.invokeLater(Main::Update));

    /**
     * Triggers the scoring sequence animation and pause.
     * 
     * When called:
     * 1. Pauses the game
     * 2. Displays the score for 500ms
     * 3. Resets paddle and ball positions to center
     * 4. Shows reset state for 1000ms
     * 5. Resumes the game
     * 
     * Executes asynchronously in a separate thread to avoid blocking the game loop.
     */
    public static void triggerScoringSequence() {
        isPaused = true;
        // Use a scheduled executor to avoid manual Thread.sleep and to
        // perform state resets on the EDT to avoid races.
        final java.util.concurrent.ScheduledExecutorService scorer = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "mw-scorer");
            t.setDaemon(true);
            return t;
        });

        // After 500ms: reset positions on EDT
        scorer.schedule(() -> {
            System.out.println("PAUSE: Resetting positions (scheduled)");
            javax.swing.SwingUtilities.invokeLater(() -> {
                Config.Paddle.LeftPaddle.resetToCenter();
                Config.Paddle.RightPaddle.resetToCenter();
                AI.reset();
                Config.Ball.resetToCenter();
            });

            // After reset, wait 1000ms then resume on EDT and shutdown scheduler
            scorer.schedule(() -> {
                System.out.println("PAUSE: Resuming game (scheduled)");
                javax.swing.SwingUtilities.invokeLater(() -> {
                    isPaused = false;
                });
                scorer.shutdown();
            }, 1000, java.util.concurrent.TimeUnit.MILLISECONDS);

        }, 500, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Main game update method called every game tick.
     * 
     * In order, performs:
     * 1. Renders the current game state
     * 2. Skips logic updates if game is paused
     * 3. Updates left paddle position based on player input
     * 4. Updates ball physics (movement, collisions, scoring)
     * 5. Updates AI paddle logic
     * 6. Updates right paddle position
     */
    public static void Update() {
        if (isPaused) {
            return;
        }
        if(Screen.hasRend   ered) {
            graphicalRender.postImage();
        }
        Config.Paddle.LeftPaddle.Move(Config.Paddle.organizeInput(Config.Paddle.LeftPaddle.input));
        Config.Ball.Move();
        AI.update();
        Config.Paddle.calculateDynamicSpeed();
        Config.Paddle.RightPaddle.Move(Config.Paddle.organizeInput(Config.Paddle.RightPaddle.input));

    }
}
