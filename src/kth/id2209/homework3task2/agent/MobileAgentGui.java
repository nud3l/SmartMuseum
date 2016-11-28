package kth.id2209.homework3task2.agent;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

import jade.gui.*;

/**
 * Created by nud3l on 11/24/16.
 * Code taken from https://www.iro.umontreal.ca/~vaucher/Agents/Jade/Mobility.html
 */

public class MobileAgentGui extends JFrame {
    private JTextField location;
    private JTextField info;
    private MobileAgent myAgent;

    public MobileAgentGui(MobileAgent a) {
        myAgent = a;
        setTitle(myAgent.getLocalName());

        // Add button and text field
        Container c = getContentPane();
        JPanel base = new JPanel();
        base.setBorder(new EmptyBorder(20,20,20,20));
        getContentPane().add(base);
        base.setLayout(new BorderLayout(0,20));
        JPanel pane = new JPanel();
        pane.setLayout(new BorderLayout(10,0));
        pane.add(new JLabel("Current location : "), BorderLayout.WEST);
        pane.add(location = new JTextField(15), BorderLayout.EAST);
        location.setEditable(false);
        location.setBackground(Color.white);
        base.add(pane, BorderLayout.NORTH);
        base.add(info = new JTextField(20), BorderLayout.SOUTH);
        info.setEditable(false);
        info.setHorizontalAlignment(JTextField.CENTER);

        setSize(200,100);
        pack();
        setResizable(false);
        Rectangle r = getGraphicsConfiguration().getBounds();
        setLocation(r.x + r.width-getWidth(), r.y);
    }

    public void setLocation(String loc){
        this.location.setText(loc);
    }

    public void setInfo(String info){
        this.info.setText(info);
    }

}
