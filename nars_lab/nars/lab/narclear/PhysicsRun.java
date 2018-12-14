/**
 * *****************************************************************************
 * Copyright (c) 2013, Daniel Murphy All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. * Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *****************************************************************************
 */
package nars.lab.narclear;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import nars.NAR;
import nars.lab.narclear.jbox2d.PhysicsController;
import nars.lab.narclear.jbox2d.PhysicsController.MouseBehavior;
import nars.lab.narclear.jbox2d.PhysicsController.UpdateBehavior;
import nars.lab.narclear.jbox2d.TestbedErrorHandler;
import nars.lab.narclear.jbox2d.TestbedState;
import nars.lab.narclear.jbox2d.j2d.DrawPhy2D;
import nars.lab.narclear.jbox2d.j2d.TestPanelJ2D;
import nars.lab.narclear.jbox2d.j2d.TestbedSidePanel;

/**
 * The entry point for the testbed application
 *
 * @author Daniel Murphy
 */
public class PhysicsRun {
    public final PhysicsController controller;
    // private static final Logger log = LoggerFactory.getLogger(TestbedMain.class);

    NAR nar;
    private final float simulationRate;
    public PhysicsRun(NAR nar, float simulationRate, PhysicsModel... tests) {
        this.nar=nar;
        this.simulationRate = simulationRate;
    // try {
        // UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
        // } catch (Exception e) {
        // log.warn("Could not set the look and feel to nimbus.  "
        // + "Hopefully you're on a mac so the window isn't ugly as crap.");
        // }
        TestbedState model = new TestbedState();
        controller = new PhysicsController(model, UpdateBehavior.UPDATE_CALLED, MouseBehavior.NORMAL,
            new TestbedErrorHandler() {
                @Override
                public void serializationError(Exception e, String message) {
                    JOptionPane.showMessageDialog(null, message, "Serialization Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
        PhysPanel panel = new PhysPanel(model, controller);

        model.setPanel(panel);
        model.setDebugDraw(new DrawPhy2D(panel, true));

        for (PhysicsModel test : tests) {
            model.addTest(test);
        }

        JFrame window = new JFrame();
        window.setTitle("NAR Physics");
        window.setLayout(new BorderLayout());
        TestbedSidePanel side = new TestbedSidePanel(model, controller);
        window.add((Component) panel, "Center");
        //window.add(new JScrollPane(side), "East");
        window.pack();
        window.setVisible(true);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        controller.ready();
    }
    
    public void keyPressed(KeyEvent e) {
        
    }
    
    class PhysPanel extends TestPanelJ2D implements KeyListener {

        public PhysPanel(final TestbedState model, final PhysicsController controller) {
            super(model,controller);
            this.addKeyListener(this);
        }
        
        @Override
            public void keyPressed(KeyEvent e) {
                PhysicsRun.this.keyPressed(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }

            @Override
            public void keyTyped(KeyEvent e) {

            }
        
    }

    /*public void start(final int fps) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                controller.setFrameRate(fps);
                controller.start();
            }
        });        
    }*/
    
    public void cycle() {          
        controller.cycle(simulationRate);
    }
}
