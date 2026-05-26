package Main;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;

/**
 * RenderObject class encapsulating graphical data for rendering.
 * 
 * Contains a shape and color pair that can be drawn to the screen.
 * Used for all game objects: paddles, ball, and potentially other elements.
 */
public class RenderObject {

    /** Geometric shape to render */
    public Shape shape;
    /** Color to fill the shape with */
    public Color color;

    /** Standard paddle color (light gray) */
    public static final Color PADDLE_COLOR = new Color(230, 230, 230);

    public static final Color BALL_COLOR = new Color(200, 50, 50);

    /**
     * Constructs a render object with a shape and color.
     * 
     * @param shape The geometric shape to render
     * @param color The fill color for the shape
     */
    public RenderObject(Shape shape, Color color) {
        this.shape = shape;
        this.color = color;
    }

    /**
     * Gets the color of this render object.
     * 
     * @return The fill color
     */
    public Color getColor() {
        return this.color;
    }

    /**
     * Gets the shape of this render object.
     * 
     * @return The geometric shape to render
     */
    public Shape getShape() {
        return this.shape;
    }

    /**
     * Applies an affine transformation to the shape of this render object.
     * @param at The affine transformation to apply (e.g., for rotation, scaling, translation)
     */
    public void setTransform(AffineTransform at) {
        this.shape = at.createTransformedShape(this.shape);
    }
}