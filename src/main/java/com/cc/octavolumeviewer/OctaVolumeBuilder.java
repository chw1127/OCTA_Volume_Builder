package com.cc.octavolumeviewer;

import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

public class OctaVolumeBuilder {
    JFrame frame;
    Volume_Viewer vv;
    JProgressBar progressBar;

    public void setProgress(int value, String msg){
        progressBar.setValue(value);
        progressBar.setString(msg);
    }
    public void run(){

        JPanel mainPanel =new JPanel();

        progressBar = new JProgressBar(0,100);
        progressBar.setString("");
        progressBar.setStringPainted(true);
        progressBar.setValue(0);
        progressBar.setPreferredSize( new Dimension(600, 20));
        progressBar.setVisible(false);

        // 线性布局
        JButton buttonOpen = new JButton("打开文件");
        buttonOpen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(new FileNameExtensionFilter("Images", "png","jpg"));
                int returnValue = fc.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fc.getSelectedFile();
                    System.out.println("Selected file: " + selectedFile.getAbsolutePath());
                    if(vv!=null){
                        mainPanel.remove(vv);
                        progressBar.setVisible(true);
                        mainPanel.repaint();
                    }

                    new Thread(new Runnable() {
                        public void run() {
                            progressBar.setVisible(true);
                            vv = new Volume_Viewer();
                            vv.run(selectedFile.getAbsolutePath(), OctaVolumeBuilder.this);
                            progressBar.setVisible(false);
                            mainPanel.add(vv, BorderLayout.CENTER);
                            mainPanel.repaint();
                            frame.validate();
                        }
                    }).start();
                }
            }
        });

        JPanel toolsPanel =new JPanel(new BorderLayout());
        toolsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        toolsPanel.add(buttonOpen,BorderLayout.WEST);
        toolsPanel.add(progressBar,BorderLayout.EAST);




        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(Color.black);
        mainPanel.setPreferredSize(new Dimension(864+200, 864));
        // 在左边添加一个面板
        JPanel gui = new JPanel();
        gui.setLayout(new BorderLayout());
        gui.add(toolsPanel, BorderLayout.NORTH);
        gui.add(mainPanel, BorderLayout.CENTER);

        frame = new JFrame("脉络膜血管网立体视觉还原");
        frame.setResizable(false);
//        frame.setSize(1080, 720);
        // 获取屏幕的大小

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                frame.dispose();
                System.exit(0);
            }
        });
        frame.setVisible(true);
        frame.getContentPane().add(gui);
        frame.pack();
        frame.validate();



        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        // 计算居中时左上角的坐标
        int x = (screenSize.width - frame.getWidth()) / 2;
        int y = (screenSize.height - frame.getHeight()) / 2;
        // 设置frame的位置
        frame.setLocation(x, y);
    }

    public static void main(String[] args) {
        try {
            // 设置外观为Substance Business Black Steel
            UIManager.setLookAndFeel(new WindowsLookAndFeel());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> {
            OctaVolumeBuilder octaVolumeBuilder =new OctaVolumeBuilder();
            octaVolumeBuilder.run();
        });

    }
}
