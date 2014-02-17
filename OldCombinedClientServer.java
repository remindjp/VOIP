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
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
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

public class Server2 extends JPanel implements ActionListener{

	private static final long serialVersionUID = 1L;
	//JFrame Stuff
	///////////////////////////////////////////////////
    private static String OK = "Connect";
    private JTextField ipaddr;
    private JTextField sourceA;
    private JTextField sourceV;
    private JTextField destA;
    private JTextField destV;
    private JTextField wait;
    public static JFrame frame;	
    
    public static String IP;
    public static Integer sourceAudioPort;
    public static Integer destAudioPort;
    public static Integer sourceVideoPort;
    public static Integer destVideoPort;
    public static Integer waitTime;
    
    public static CountDownLatch done;
    public Server2(JFrame f) {
    	done = new CountDownLatch(1);
    	//create
        ipaddr = new JTextField(8);
        ipaddr.setActionCommand(OK);
        ipaddr.addActionListener(this);
        ipaddr.setText("76.22.60.23");
        
        sourceA = new JTextField(4);
        sourceA.setActionCommand(OK);
        sourceA.addActionListener(this);
        sourceA.setText("50001");
        
        sourceV = new JTextField(4);
        sourceV.setActionCommand(OK);
        sourceV.addActionListener(this);
        sourceV.setText("50003");
        
        destA = new JTextField(4);
        destA.setActionCommand(OK);
        destA.addActionListener(this);
        destA.setText("50000");
        
        destV = new JTextField(4);	
        destV.setActionCommand(OK);
        destV.addActionListener(this);
        destV.setText("50002");
        
        wait = new JTextField(5);	
        wait.setActionCommand(OK);
        wait.addActionListener(this);
        wait.setText("5000");
        
        //labels
        JLabel label = new JLabel("IP");
        label.setLabelFor(ipaddr);
        
        JLabel label2 = new JLabel("audio port");
        label.setLabelFor(sourceA);
 
        JLabel label3 = new JLabel("video port");
        label.setLabelFor(sourceV);
 
        JLabel label4 = new JLabel("Dest audio port");
        label.setLabelFor(destA);
 
        JLabel label5 = new JLabel("Dest video port");
        label.setLabelFor(destV);
        
        JLabel label6 = new JLabel("delay (ms)");
        label.setLabelFor(wait);       
        
        JComponent buttonPane = createButtonPanel();
 
        //add
        JPanel textPane = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        textPane.add(label);
        textPane.add(ipaddr);
        textPane.add(label2);
        textPane.add(sourceA);
        textPane.add(label3);
        textPane.add(sourceV);
        textPane.add(label4);
        textPane.add(destA);
        textPane.add(label5);
        textPane.add(destV);
        textPane.add(label6);
        textPane.add(wait);     
        
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
 
        if (OK.equals(cmd)) { //Process the password.
        	
            IP = ipaddr.getText();
            sourceAudioPort = Integer.parseInt(sourceA.getText());
            sourceVideoPort = Integer.parseInt(sourceV.getText());
            destAudioPort = Integer.parseInt(destA.getText());
            destVideoPort = Integer.parseInt(destV.getText());
            waitTime = Integer.parseInt(wait.getText());
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
        frame.setContentPane(new Server2(frame));
        frame.pack();
        frame.setVisible(true);
        done.await();
        IP="192.168.1.3";
        (new OpenSocket(50000)).start();
        (new VideoSocket(50001)).start();    
        
//        (new OpenSocket(sourceAudioPort)).start();
//		(new VideoSocket(sourceVideoPort)).start();
		System.out.println(sourceAudioPort + " " + sourceVideoPort);
        
		try {
			Thread.sleep(waitTime);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		(new JoinSocket(50000, IP)).start();
//		(new VideoJoin(50000, IP)).start();
 //       (new JoinSocket(destAudioPort, IP)).start();
 //       (new VideoJoin(destVideoPort, IP)).start(); 
        
        
 //       Process p = Runtime.getRuntime().exec("netsh advfirewall firewall add rule name= \"CS5390 VOIP\" dir=in action=allow protocol=TCP localport=50000"); 
	}
} 	

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
			socketV = new Socket(dest, port);
	        ObjectOutputStream objectOutputV = new ObjectOutputStream(socketV.getOutputStream());    	
			

	        ObjectInputStream ois = new ObjectInputStream(socketV.getInputStream());	
	        (new mini(ois)).run();
	        
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
		        //outbyte byte
		        objectOutputV.writeObject(matbyte.toArray());
		        objectOutputV.reset();
		    }
		    
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}

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
	        
	        (new mini(outputV)).start();
	        
	        JFrame f = new JFrame();
	        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			BufferedImage firstImage = ImageIO.read(new File("test.jpg"));
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
		} catch (IOException | ClassNotFoundException e) {
			try {
				ss1.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}
	
	public class mini extends Thread {
		ObjectOutputStream outputV;
		public mini(ObjectOutputStream outputV) {
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
			        //outbyte byte
			        objectOutputV.writeObject(matbyte.toArray());
			        objectOutputV.reset();
			    }
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}	
	
}

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
			try {
				ss.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}
	
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}	

}



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
	
	//76.22.60.23
	//arnie 24.19.170.34
	//hb 192.168.1.3
	//me 76.186.210.4
	
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
			// TODO Auto-generated catch block
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
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}		
}

