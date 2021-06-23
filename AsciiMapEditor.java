import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

public class AsciiMapEditor extends JFrame {

	private BufferedImage hintergrund;
	private double hintergrundFaktor = 1/1.8*2;
	public double x=0;
	public double y=0;
	public int dragX;
	public int dragY;
	public double faktor=1;
	public char[][] zeichen;
	public char[][] backup;	
	public Font font;
	public int w;
	public int h;
	public int zw;
	public int zh;
	public int aktuellesZeichenX=-1;
	public int aktuellesZeichenY=-1;
	private BufferedImage imageZeichen;
	private BufferedImage imageFarben;
	private int ascent;
	private JPanel panel;
	public char aktZeichen = ' ';
	public Color[] farben = new Color[256];
	public int modus=0;
//	private File bilddatei = new File("d:/dsa/7drl/havena.png");
	private File bilddatei = new File("d:/dsa/7drl/aventurien.jpg");
	
	public static void main(String[] args) throws Exception {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("select an image");
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
		        "Bilddateien", "jpg", "png");
	    chooser.setFileFilter(filter);
	    int returnVal = chooser.showOpenDialog(null);
		new AsciiMapEditor(chooser.getSelectedFile());
	}
	
	public AsciiMapEditor(File bilddatei) throws Exception {
		
		this.bilddatei = bilddatei;
		hintergrund = ImageIO.read(bilddatei);
		
		font = new Font(Font.MONOSPACED, Font.BOLD, 12);
		setFont(font);
		FontMetrics fontMetrics = getFontMetrics(font);
		zw = fontMetrics.charWidth('A');
		zh = fontMetrics.getHeight();
		ascent = fontMetrics.getAscent();
		w = (int) Math.ceil(hintergrund.getWidth()*hintergrundFaktor/zw);
		h = (int) Math.ceil(hintergrund.getHeight()*hintergrundFaktor/zh);
		zeichen = new char[h][w];
		farben[' '] = new Color(0,0,0,0);
		
		imageZeichen = new BufferedImage(w*zw,h*zh,BufferedImage.TYPE_INT_ARGB);
		imageFarben = new BufferedImage(w*zw,h*zh,BufferedImage.TYPE_INT_ARGB);
		
		for( int i=0; i<w; i++)
			for( int j=0; j<h; j++ )
				zeichen[j][i]=' ';
		
		zeichneZeichen();
		
		panel = new JPanel(true) {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D g2 = (Graphics2D)g;
				AffineTransform oldTransform = g2.getTransform();
				AffineTransform transform = new AffineTransform(faktor, 0, 0, faktor, x,y);
				g2.transform(transform );
				
				if(modus<3)
					g2.drawImage(hintergrund, 0,0,(int)(hintergrund.getWidth()*hintergrundFaktor),(int)(hintergrund.getHeight()*hintergrundFaktor), null);
				
				g2.drawImage(imageZeichen, 0, 0,null);
				g2.drawRect(zw*aktuellesZeichenX, zh*aktuellesZeichenY, zw, zh);
			}
		};
		panel.setFont(font);
		panel.setFocusable(true);
		
		panel.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				
				if(e.getKeyCode()==KeyEvent.VK_F1) {
					modus=(modus+1)%5;
					zeichneZeichen();
					repaint();
				}
				else if(e.getKeyCode()==KeyEvent.VK_S && e.isControlDown()) {
					save();
				}
				else if(e.getKeyCode()==KeyEvent.VK_L && e.isControlDown()) {
					load();
				}
				else if(e.getKeyCode()==KeyEvent.VK_Z && e.isControlDown()) {
					undo();
//				} else {
//					try {
//						aktZeichen = e.getKeyChar();
//						if( farben[aktZeichen]==null ) {
//							farben[aktZeichen] = new Color(hintergrund.getRGB((int)((dragX-x)/faktor/hintergrundFaktor),(int)((dragY-y)/faktor/hintergrundFaktor)));
//						}
//					} catch (Exception ex) {
//						// TODO: handle exception
//					}
				}
			}
			
			public void keyTyped(KeyEvent e) {
				aktZeichen = e.getKeyChar();
				if( farben[aktZeichen]==null ) {
					farben[aktZeichen] = new Color(hintergrund.getRGB((int)((dragX-x)/faktor/hintergrundFaktor),(int)((dragY-y)/faktor/hintergrundFaktor)));
				}
			}
		});
		
		panel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				dragX=e.getX();
				dragY=e.getY();
				aktuellesZeichenX = (int) Math.floor((e.getX()-x)/faktor/zw);
				aktuellesZeichenY = (int) Math.floor((e.getY()-y)/faktor/zh);
				if( (e.getModifiersEx()&e.BUTTON1_DOWN_MASK)!=0) {
					zeichen[aktuellesZeichenY][aktuellesZeichenX] = aktZeichen;
					zeichneZeichen(aktuellesZeichenX, aktuellesZeichenY);
					repaint();
				} else if( (e.getModifiersEx()&e.BUTTON2_DOWN_MASK)!=0) {
					floodFill(aktuellesZeichenX,aktuellesZeichenY, zeichen[aktuellesZeichenY][aktuellesZeichenX], new Stack<>());
					zeichneZeichen();
					panel.repaint();
				}
			}
		});
		
		panel.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				dragX=e.getX();
				dragY=e.getY();
				aktuellesZeichenX = (int) Math.floor((dragX-x)/faktor/zw);
				aktuellesZeichenY = (int) Math.floor((dragY-y)/faktor/zh);
				panel.repaint();
			}
			@Override
			public void mouseDragged(MouseEvent e) {
				aktuellesZeichenX = (int) Math.floor((e.getX()-x)/faktor/zw);
				aktuellesZeichenY = (int) Math.floor((e.getY()-y)/faktor/zh);
				if( (e.getModifiersEx()&e.BUTTON1_DOWN_MASK)!=0) {
					zeichen[aktuellesZeichenY][aktuellesZeichenX] = aktZeichen;
					zeichneZeichen(aktuellesZeichenX, aktuellesZeichenY);
				}
				if( (e.getModifiersEx()&e.BUTTON3_DOWN_MASK)!=0) {
					x+=e.getX()-dragX;
					y+=e.getY()-dragY;
				}
				dragX=e.getX();
				dragY=e.getY();
				panel.repaint();
			}
		});
		panel.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				
				double alterFaktor= faktor;
				faktor*=Math.pow(1.2,-e.getPreciseWheelRotation());		

				x = e.getX()-(e.getX()-x)*faktor/alterFaktor;
				y = e.getY()-(e.getY()-y)*faktor/alterFaktor;
				
				panel.repaint();
			}
		});
		
		this.add(panel);
		setVisible(true);
		setSize(1600,1000);
		setLocationRelativeTo(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	protected void floodFill(int px, int py, char c, Stack<int[]> stack) {
		backup = new char[zeichen.length][zeichen[0].length];
		for( int i=0; i<zeichen.length; i++ )
			for( int j=0; j<zeichen[i].length; j++ )
				backup[i][j] = zeichen[i][j];
		while(true) {
			if( zeichen[py][px]==c ) {
				zeichen[py][px]=aktZeichen;
				if( px>0 ) stack.add(new int[] {px-1,py});
				if( px<w-1) stack.add(new int[] {px+1,py});
				if( py>0 ) stack.add(new int[] {px,py-1});
				if(py<h-1) stack.add(new int[] {px,py+1});
			}
			if( stack.empty() )
				return;
			int[] p = stack.pop();
			px = p[0];
			py = p[1];
		}
	}
	
	private void undo() {
		for( int i=0; i<zeichen.length; i++ )
			for( int j=0; j<zeichen[i].length; j++ )
				zeichen[i][j] = backup[i][j];
		zeichneZeichen();
		panel.repaint();
	}

	private void zeichneZeichen() {
		Graphics2D g = (Graphics2D)imageZeichen.getGraphics();
		g.setComposite(AlphaComposite.Src);
		g.setColor(new Color(1,1,1,1));
		g.fillRect(0, 0, imageZeichen.getWidth(), imageZeichen.getHeight());
		g.setFont(font);
		for( int i=0; i<h; i++ ) {
			if(modus==0 || modus==2 || modus==4)
			for( int j=0; j<w; j++ ) {
				g.setColor(farben[zeichen[i][j]]);
				g.fillRect(j*zw, i*zh, zw, zh);
			}
			g.setColor(new Color(1,1,1));
			if(modus!=2)
				g.drawString(String.valueOf(zeichen[i]),0,i*zh+ascent);
		}
	}
	
	private void zeichneZeichen(int x, int y) {
		Graphics2D g = (Graphics2D)imageZeichen.getGraphics();
		g.setComposite(AlphaComposite.Src);
		g.setColor(new Color(1,1,1,1));
		g.fillRect(0, y*zh, imageZeichen.getWidth(), zh);
		g.setFont(font);
		g.setColor(new Color(1,1,1));
		g.drawString(String.valueOf(zeichen[y]),0,y*zh+ascent);
		if(modus==0 || modus==2 || modus==4)
			for( int j=0; j<w; j++ ) {
				g.setColor(farben[zeichen[y][j]]);
				g.fillRect(j*zw, y*zh, zw, zh);
			}
		g.setColor(new Color(1,1,1));
		if(modus!=2)
			g.drawString(String.valueOf(zeichen[y]),0,y*zh+ascent);
	}
	
	
	public void save() {
		String text = "var "+bilddatei.getName().substring(0,bilddatei.getName().lastIndexOf('.'))+" = [\n";
		text+=Stream.of(zeichen).map(String::valueOf).collect(Collectors.joining("\",\n\t\"", "\t\"", "\"\n"));
		text+="];\n";
		text+="\nvar farben = {";
		boolean ersteFarbe=true;
		for( char i=0; i<farben.length; i++) {
			if( farben[i]!=null ) {
				if( !ersteFarbe )
					text+=",";
				text+="\n\t'"+i+"': ["+farben[i].getRed()+","+farben[i].getGreen()+","+farben[i].getBlue()+"]";
				ersteFarbe=false;
			}
		}
		text+="\n};";
		
		try {
			File textdatei = new File(bilddatei.getParentFile(),bilddatei.getName().substring(0,bilddatei.getName().lastIndexOf('.'))+".js");
			Files.writeString(Paths.get(textdatei.getAbsolutePath()), text, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
			
			int counter=1;
			while(new File(bilddatei.getParentFile(),bilddatei.getName().substring(0,bilddatei.getName().lastIndexOf('.'))+counter+".js").exists())
				counter++;
			
			textdatei = new File(bilddatei.getParentFile(),bilddatei.getName().substring(0,bilddatei.getName().lastIndexOf('.'))+counter+".js");
			Files.writeString(Paths.get(textdatei.getAbsolutePath()), text, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Datei wurde gespeichert.");
	}

	public void load() {
		
		try {
			File textdatei = new File(bilddatei.getParentFile(),bilddatei.getName().substring(0,bilddatei.getName().lastIndexOf('.'))+".js");
			List<String> lines = Files.readAllLines(Paths.get(textdatei.getAbsolutePath()), StandardCharsets.UTF_8);
			for( int i=0; i<h; i++ ) {
				zeichen[i] = lines.get(i+1).replaceAll("\t\"(.*)\",?", "$1").toCharArray();
			}
			
			Pattern pattern = Pattern.compile("\\'(.)\\': \\[([0-9]+),([0-9]+),([0-9]+)\\],?");
			farben = new Color[farben.length];
			for( int i=h; i<lines.size(); i++) {
				Matcher matcher = pattern.matcher(lines.get(i));
				if( matcher.find() ) {
					farben[matcher.group(1).charAt(0)] = new Color(Integer.parseInt(matcher.group(2)),Integer.parseInt(matcher.group(3)),Integer.parseInt(matcher.group(4)));
				}
			}
			farben[' '] = new Color(0,0,0,0);
			zeichneZeichen();
			repaint();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
