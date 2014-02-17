//Jingpeng Wu CS5390 Networks VOIP Project Fall 2013
//Client

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

public class Client extends JPanel implements ActionListener{

    private static final long serialVersionUID = 1L;
    //JFrame Stuff
    ///////////////////////////////////////////////////
    private static String OK = "Connect";
    private JTextField ipaddr;
    private JTextField destA;
    private JTextField destV;
    private JTextField wait;
    public static JFrame frame;   
   
    public static String IP;
    public static Integer destAudioPort;
    public static Integer destVideoPort;
   
    public static CountDownLatch done;
    public Client(JFrame f) {
        done = new CountDownLatch(1);
        //create
        ipaddr = new JTextField(8);
        ipaddr.setActionCommand(OK);
        ipaddr.addActionListener(this);
        ipaddr.setText("76.22.60.23");
       
       
        destA = new JTextField(4);
        destA.setActionCommand(OK);
        destA.addActionListener(this);
        destA.setText("50000");
       
        destV = new JTextField(4);   
        destV.setActionCommand(OK);
        destV.addActionListener(this);
        destV.setText("50001");
       
        wait = new JTextField(5);   
        wait.setActionCommand(OK);
        wait.addActionListener(this);
        wait.setText("5000");
       
        //labels
        JLabel label = new JLabel("IP");
        label.setLabelFor(ipaddr);
       
 
        JLabel label3 = new JLabel("Dest audio port");
        label.setLabelFor(destA);
 
 
        JLabel label5 = new JLabel("Dest video port");
        label.setLabelFor(destV);
       
   
        JComponent buttonPane = createButtonPanel();
 
        //add
        JPanel textPane = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        textPane.add(label);
        textPane.add(ipaddr);
        textPane.add(label3);
        textPane.add(destA);
        textPane.add(label5);
        textPane.add(destV);    
       
        add(textPane);
        add(buttonPane);
    }
 
    protected JComponent createButtonPanel() {
        JPanel p = new JPanel(new GridLayout(1,0));
        JButton okButton = new JButton("Connect");
        okButton.setActionCommand(OK);
        okButton.addActionListener(this);
        p.add(okButton);
        return p;
    }
    
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
 
        if (OK.equals(cmd)) { 
           
            IP = ipaddr.getText();
            destAudioPort = Integer.parseInt(destA.getText());
            destVideoPort = Integer.parseInt(destV.getText());
            done.countDown();
        }
        frame.setVisible(false);
        frame.dispose();
    }
  
/////////////////////////////////////////////////////////   
    //Not used, didn't work on all laptops tested
    //netsh advfirewall firewall add rule name="outVOIP" protocol=TCP dir=out localport=50000 action=allow
    //netsh advfirewall firewall add rule name="inboundVOIP" protocol=TCP dir=in localport=50000 security=authdynenc action=allow
    public static void main(String[] args) throws IOException, ClassNotFoundException, LineUnavailableException, InterruptedException {

        frame = new JFrame("configuration");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new Client(frame));
        frame.pack();
        frame.setVisible(true);
        done.await();
       
       (new JoinSocket(destAudioPort,IP)).start();
       (new VideoJoin(destVideoPort,IP)).start();
       
    
    }
}    

//Socket that connects to the ServerSocket and is accessed for video datastreams.
class VideoJoin extends Thread {
    Socket socketV;
    ObjectInputStream ois;

    int port;
    String dest;
    public VideoJoin (int port, String dest){
        this.port = port;
        this.dest = dest;
    }
    public void run() {
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            socketV = new Socket(dest, port);
            ObjectOutputStream objectOutputV = new ObjectOutputStream(socketV.getOutputStream());       

            ObjectInputStream ois = new ObjectInputStream(socketV.getInputStream());
            
            //Threaded to run while(true) loop
            (new mini(ois)).run();

            //Single camera id = 0
            VideoCapture camera = new VideoCapture(0);
            Thread.sleep(1000);
   
            while(true) {
                Mat frame = new Mat();
                camera.read(frame);
                //Encodes the frame into a matbyte format
                MatOfByte matbyte = new MatOfByte(new byte[100000]);
                Highgui.imencode(".jpg", frame, matbyte);
                //outbyte byte
                objectOutputV.writeObject(matbyte.toArray());
                objectOutputV.reset();
            }
           
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }           
    }   
   
    public class mini extends Thread {
        ObjectInputStream ois;
        public mini(ObjectInputStream ois) {

            this.ois = ois;
        }
       
        public void run() {
            try {
                JFrame f = new JFrame();
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                BufferedImage firstImage;

                firstImage = ImageIO.read(new File("test.jpg"));

                JLabel label = new JLabel(new ImageIcon(firstImage));       
                f.getContentPane().add(label);
                f.pack();
                f.setVisible(true);

                //updating
                while(true) {
                    byte[] temp = (byte[]) ois.readObject();
                    InputStream in = new ByteArrayInputStream(temp);
                    BufferedImage image = ImageIO.read(in);
                    label.setIcon(new ImageIcon(image));
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}

//Socket that connects to the ServerSocket and is accessed for audio datastreams.
class JoinSocket extends Thread {
    Socket socket2;
    ObjectOutputStream objectOutput;
    int port;
    String dest;
    ObjectInputStream ois;
    public JoinSocket (int port, String dest){
        this.port = port;
        this.dest = dest;
    }

    public void run() {
        try {
            socket2 = new Socket(dest, port);
            objectOutput = new ObjectOutputStream(socket2.getOutputStream());
            AudioFormat format = new AudioFormat(48000,16,1,true,false);
           
            ois = new ObjectInputStream(socket2.getInputStream());   
            (new mini(ois)).start();
           
            //mic
            TargetDataLine line = AudioSystem.getTargetDataLine(format);
            line.open(format);
            line.start();
           
            byte[] temp = new byte[48000];       
            while (true) {
                int count = line.read(temp, 0, temp.length);
                if(count >0) {                   
                    objectOutput.writeObject(temp);
                    objectOutput.reset();
                }  
            }           
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }   
       
       
    }
    class mini extends Thread {
        ObjectInputStream ois;
        public mini (ObjectInputStream ois) {
            this.ois = ois;
        }
        public void run() {
            try {
                AudioFormat format = new AudioFormat(48000,16,1,true,false);
                //audio
                SourceDataLine lineout = AudioSystem.getSourceDataLine(format);
                lineout.open(format);
                lineout.start();   
                byte[] temp2 = (byte[])ois.readObject();      
                while(true) {
                    temp2 = (byte[])ois.readObject();
                    lineout.write(temp2, 0, 48000);
                }                
            } catch (LineUnavailableException e) {
                e.printStackTrace();           
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }       
}

