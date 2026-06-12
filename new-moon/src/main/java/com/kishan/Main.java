package com.kishan;

import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.SwingUtilities;

import com.kishan.common.IThread;

public class Main implements Runnable {
    protected static GraphicalRender graphicalRender;
    public static Screen screen;
    
    public static void main(String[] args){
        screen = new Screen(new Dimension(300, 300), true, false, true);
        graphicalRender = new GraphicalRender(screen);


        screen.startRenderLoop(1000 / 60);
        screen.setVisible(true);
        updateThread.startThread(1000 / 60);

        screen.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
            }

            public void keyReleased(KeyEvent e) {
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

    private static boolean isPaused = true;
    static final IThread updateThread = IThread.create(() -> SwingUtilities.invokeLater(Main::Update));


    @Override
    public void run() {
        updateThread.startThread(1000/30);
    }

    public static void Update() {
        if (!isPaused) {
            return;
        }
        //TODO - update game state, move player/astroids, etc.
    }
}