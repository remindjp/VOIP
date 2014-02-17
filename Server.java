//Jingpeng Wu CS5390 Networks VOIP Project Fall 2013
//Server

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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
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

public class Server extends JPanel implements ActionListener{

	private static final long serialVersionUID = 1L;
	//JFrame Stuff
	///////////////////////////////////////////////////
    private static String OK = "Connect";
    private JTextField ipaddr;
    private JTextField sourceA;
    private JTextField sourceV;
    public static JFrame frame;	
    
    public static Integer sourceAudioPort;
    public static Integer sourceVideoPort;
    
    public static CountDownLatch done;
    public Server(JFrame f) {
    	done = new CountDownLatch(1);
    	//create
        ipaddr = new JTextField(8);
        ipaddr.setActionCommand(OK);
        ipaddr.addActionListener(this);
        ipaddr.setText("192.168.1.1");
        
        sourceA = new JTextField(4);
        sourceA.setActionCommand(OK);
        sourceA.addActionListener(this);
        sourceA.setText("50000");
        
        sourceV = new JTextField(4);
        sourceV.setActionCommand(OK);
        sourceV.addActionListener(this);
        sourceV.setText("50001");

        //labels
        
        JLabel label2 = new JLabel("audio port");
        label2.setLabelFor(sourceA);
 
        JLabel label3 = new JLabel("video port");
        label3.setLabelFor(sourceV);  
        
        JComponent buttonPane = createButtonPanel();
 
        //add
        JPanel textPane = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        textPane.add(label2);
        textPane.add(sourceA);
        textPane.add(label3);
        textPane.add(sourceV);
        
        add(textPane);
        add(buttonPane);
    }
 
    protected JComponent createButtonPanel() {
        JPanel p = new JPanel(new GridLayout(1,0));
        JButton okButton = new JButton("Start");
        okButton.setActionCommand(OK);
        okButton.addActionListener(this);
        p.add(okButton);
        return p;
    }
     
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
 
        if (OK.equals(cmd)) { //Process the password.
        	
            sourceAudioPort = Integer.parseInt(sourceA.getText());
            sourceVideoPort = Integer.parseInt(sourceV.getText());
            done.countDown();
        }
        frame.setVisible(false);
        frame.dispose();
    }
   
/////////////////////////////////////////////////////////	
	//netsh advfirewall firewall add rule name="allow80" protocol=TCP dir=out localport=80 action=block outgoing
	//netsh advfirewall firewall add rule name="Require Encryption for Inbound TCP/80" protocol=TCP dir=in localport=80 security=authdynenc action=allow
	public static void main(String[] args) throws IOException, ClassNotFoundException, LineUnavailableException, InterruptedException {

    	frame = new JFrame("configuration");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new Server(frame));
        frame.pack();
        frame.setVisible(true);
        done.await();   
  
        (new OpenSocket(sourceAudioPort)).start();
		(new VideoSocket(sourceVideoPort)).start();
        
	}
} 	


//ServerSocket that accepts a socket for videostream data
class VideoSocket extends Thread {
	ObjectOutputStream objectOutputV;
	ServerSocket ss1;
	Socket socket1;
	int port;
	public VideoSocket (int port){
		this.port = port; 
	}
	
	public void run() {
		try {
			//Setup Server
			InetAddress ip = InetAddress.getLocalHost();
			System.out.println(ip);
			ss1 = new ServerSocket(port, 0, ip);
			ss1.setReuseAddress(true);
		    socket1 = ss1.accept();
	        ObjectInputStream ois = new ObjectInputStream(socket1.getInputStream());	
	        ObjectOutputStream outputV = new ObjectOutputStream(socket1.getOutputStream());
	        
	        (new miniV(outputV)).start();
	        
	        JFrame f = new JFrame();
	        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			BufferedImage firstImage = ImageIO.read(new File("test.jpg"));
	        JLabel label = new JLabel(new ImageIcon(firstImage));        
	        f.getContentPane().add(label);
	        f.pack();
	        f.setVisible(true); 

	        //Recieve video data and update frames to make a "live video"
	        while(true) {
		        byte[] temp = (byte[]) ois.readObject();
		        InputStream in = new ByteArrayInputStream(temp);
		        BufferedImage image = ImageIO.read(in);
	        	label.setIcon(new ImageIcon(image));
	        } 
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	//used to send webcam data
	public class miniV extends Thread {
		ObjectOutputStream outputV;
		public miniV(ObjectOutputStream outputV) {
			this.outputV = outputV;
		}
		public void run(){
			try {
			    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
			    
			    //Single camera id = 0
			    VideoCapture camera = new VideoCapture(0);
				Thread.sleep(1000);
	
				while(true) {
			        Mat frame = new Mat();
			        camera.read(frame); 
			        //Encodes the frame into a matbyte format
			        MatOfByte matbyte = new MatOfByte(new byte[100000]);
			        Highgui.imencode(".jpg", frame, matbyte);
			        
			        objectOutputV.writeObject(matbyte.toArray());
			        objectOutputV.reset();
			    }
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}	
	
}

//ServerSocket that accepts a socket for audiostream data
class OpenSocket extends Thread {
	ObjectOutputStream objectOutput;
	ServerSocket ss;
	Socket socket;
	int port;
	public OpenSocket (int port){
		this.port = port; 
	}
	public void run(){

		AudioFormat format = new AudioFormat(48000,16,1,true,false);
		try {
			//Setup Server
			InetAddress ip = InetAddress.getLocalHost();
			System.out.println(ip);
			ss = new ServerSocket(port, 0, ip);
			ss.setReuseAddress(true);
		    socket = ss.accept();
			
	        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());		

	        objectOutput = new ObjectOutputStream(socket.getOutputStream());	        

	        (new mini(objectOutput)).start();

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
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//Records mic audio
	class mini extends Thread {
		ObjectOutputStream objectOutput;
		public mini (ObjectOutputStream objectOutput) {
			this.objectOutput =objectOutput;
		}
		public void run() {
			try {
				AudioFormat format = new AudioFormat(48000,16,1,true,false);
				//mic
				TargetDataLine line;
				line = AudioSystem.getTargetDataLine(format);
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
			} catch (LineUnavailableException e) {
				e.printStackTrace();			
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}	

}
