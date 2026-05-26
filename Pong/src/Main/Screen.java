package Main;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Screen class managing the game window and rendering context.
 * 
 * Responsible for:
 * - Creating and configuring the game window
 * - Managing the rendering loop
 * - Handling window decorations and user events
 * - Displaying rendered images to screen
 * - Coordinating shutdown of render and update loops
 */
public class Screen extends JFrame {

    /** Image buffer for next frame to render */
    public Image nextRender;
    /** Flag indicating if at least one frame has been rendered */
    private boolean hasRendered = false;

    /**
     * Constructs a game window with specified configuration.
     * 
     * @param size              The window size
     * @param decorated         Whether to show window decorations (title bar, etc.)
     * @param resizable         Whether the window can be resized
     * @param pauseOnNoneActive Whether to pause when window loses focus
     */
    public Screen(Dimension size, boolean decorated, boolean resizable, boolean pauseOnNoneActive) {
        getContentPane().setPreferredSize(size);

        setResizable(resizable);
        System.out.println(resizable ? "Window Set to Resizable" : "Window Set to Non-Resizable");
        System.out.println(
                pauseOnNoneActive ? "Window Set to Pause on Non-Active" : "Window Set to Continue on Non-Active");
        addStopThreadsOnCloseListener();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setUndecorated(!decorated);
        if (!decorated) {
            addCloseOnRightClickListener();
        }
        pack();
    }

    /** Executor service for the render loop */
    private final ScheduledExecutorService renderExecutor = Executors.newSingleThreadScheduledExecutor();
    /** Future representing the scheduled render loop task */
    private ScheduledFuture<?> renderFuture;

    /**
     * Gets the content pane dimensions (usable game area).
     * 
     * @return Dimensions of the content area
     */
    public Dimension getContentSize() {
        return getContentPane().getSize();
    }

    /**
     * Starts the render loop at a fixed rate.
     * 
     * The render loop repeatedly calls updateRender() at the specified interval.
     * 
     * @param periodMillis The period between renders in milliseconds (e.g.,
     *                     1000/120 for 120 FPS)
     */
    public void startRenderLoop(long periodMillis) {
        stopRenderLoop();

        renderFuture = renderExecutor.scheduleAtFixedRate(() -> {
            SwingUtilities.invokeLater(this::updateRender);
        }, 0, periodMillis, TimeUnit.MILLISECONDS);
        System.out.println("Starting Render Loop " + renderFuture.hashCode());
    }

    /**
     * Stops the render loop.
     * 
     * Cancels the scheduled render tasks and cleans up resources.
     */
    public void stopRenderLoop() {
        if (renderFuture != null && !renderFuture.isDone()) {
            System.out.println("Stopping Render Loop " + renderFuture.hashCode());

            renderFuture.cancel(false);
        }
        renderFuture = null;

    }

    /**
     * Checks if the render loop is currently running.
     * 
     * @return true if render loop is active, false otherwise
     */
    public boolean isRenderLoopRunning() {
        return renderFuture != null && !renderFuture.isDone();
    }

    private void addCloseOnRightClickListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    System.exit(0);
                }
            }
        });
    }

    private void addStopThreadsOnCloseListener() {
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                stopRenderLoop();
                Main.stopUpdateLoop();
                System.exit(0);
            }
        });
    }

    /**
     * Paints the current frame to the window.
     * 
     * Called automatically by Swing and also by the render loop.
     * Draws the nextRender image to the screen after accounting for insets.
     * 
     * @param g The Graphics context
     */
    @Override
    public void paint(Graphics g) {
        g.translate(getInsets().right, getInsets().top);
        g.drawImage(nextRender, 0, 0, null);
    }

    /**
     * Sets the image to render on the next frame.
     * 
     * @param img The buffered image to display
     */
    public void setRenderImage(Image img) {
        nextRender = img;
    }

    public void updateRender() {
        if (hasRendered) {
            return;
        } else {
            repaint();
        }
    }

}
