 package org.ic.tennistrader.ui;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import org.apache.log4j.Logger;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.events.MouseEvent;
 import org.eclipse.swt.events.MouseListener;
 import org.eclipse.swt.events.MouseTrackListener;
 import org.eclipse.swt.events.PaintEvent;
 import org.eclipse.swt.events.PaintListener;
 import org.eclipse.swt.graphics.Color;
 import org.eclipse.swt.graphics.FontMetrics;
 import org.eclipse.swt.graphics.Image;
 import org.eclipse.swt.graphics.Point;
 import org.eclipse.swt.graphics.Rectangle;
 import org.eclipse.swt.widgets.Button;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.swt.widgets.Event;
 import org.eclipse.swt.widgets.Label;
 import org.eclipse.swt.widgets.Listener;
 import org.eclipse.swt.widgets.Monitor;
 import org.eclipse.swt.widgets.ProgressBar;
 import org.eclipse.swt.widgets.Shell;
 import org.eclipse.swt.widgets.Text;
 
 import org.ic.tennistrader.Main;
 import org.ic.tennistrader.authentication.BetfairAuthenticator;
 import org.pushingpixels.trident.Timeline;
 
 public class LoginShell {
 
 	List<Listener> loginSucces = new ArrayList<Listener>();
 
 	private Shell loginShell;
 
 	public static Logger log = Logger.getLogger(LoginShell.class);
 
 	private static final String TITLE = "Tennis Trader Login";
 
 	public static final String SUCCESS = "Login successful! Please wait...";
 
 	public static final String FAIL = "Login failed! Please try again...";
 
 	private ProgressBar bar;
 
 	/* Bar Text */
 	private TextUpdater textUpdater;
 	
 	/* Success/Fail Label */
 	private Label resultLabel;
 	private Image accept;
 	private Image deny;
	private boolean visible = false;
 
 	public Shell show() {
 		return loginShell;
 	}
 
 	public LoginShell(final Display display) {
 		this.loginShell = new Shell(display, SWT.NO_TRIM);// SWT.TRANSPARENCY_ALPHA);
 		loginShell.setSize(600, 350);
 		loginShell.setBackgroundMode(SWT.INHERIT_DEFAULT);
 
 
 		final Image loginImg = new Image(loginShell.getDisplay(),
 				"images/login/login.png");
 
 		loginShell.setBackgroundImage(loginImg);
 
 		// Region region = new Region();
 		// region.add(circle(67, 67, 67));
 		// define the shape of the shell using setRegion
 		// loginShell.setRegion(region);
 
 		Listener l = new Listener() {
 			Point origin;
 
 			public void handleEvent(Event e) {
 				switch (e.type) {
 				case SWT.MouseDown:
 					origin = new Point(e.x, e.y);
 					break;
 				case SWT.MouseUp:
 					origin = null;
 					break;
 				case SWT.MouseMove:
 					if (origin != null) {
 						Point p = display.map(loginShell, null, e.x, e.y);
 						loginShell.setLocation(p.x - origin.x, p.y - origin.y);
 					}
 					break;
 				}
 			}
 		};
 		loginShell.addListener(SWT.MouseDown, l);
 		loginShell.addListener(SWT.MouseUp, l);
 		loginShell.addListener(SWT.MouseMove, l);
 
 		Rectangle rect = loginShell.getClientArea();
 
 		loginShell.setText(TITLE);
 
 		final Text username = new Text(loginShell, SWT.NONE);
 		username.setToolTipText("Please input your Betfair account login here");
 		username.setText("username");
 		username.setBounds(180, 165, 300, 30);
 		username.addMouseListener(new HoverListener(username));
 
 		final Text password = new Text(loginShell, SWT.PASSWORD);
 		password.setToolTipText("Please input your Betfair account password here");
 		password.setText("password");
 		password.setBounds(180, 210, 300, 30);
 		password.addMouseListener(new HoverListener(password));
 
 		Button login = makeLoginButton(display);
 
 		Button cancelButton = makeCancelButton(display);
 		cancelButton.addListener(SWT.Selection, new Listener() {
 			public void handleEvent(Event event) {
 				loginShell.dispose();
 			}
 		});
 		
 		login.addListener(SWT.Selection, new Listener() {
 			public void handleEvent(Event event) {
 				String user = username.getText();
 				if (BetfairAuthenticator.checkLogin(user, password.getText())) {
 					Main.username = user;
 					setSuccessLabel(true);
 					updateResult(SUCCESS);
 					handleLoginSuccess();
 				} else {
 					setSuccessLabel(false);
 					updateResult(FAIL);
 				}
 			}
 		});
 		
 		resultLabel = makeResultLabel(display);
 
 		// Center the login screen
 		Monitor primary = display.getPrimaryMonitor();
 		Rectangle bounds = primary.getBounds();
 
 		int x = bounds.x + (bounds.width - rect.width) / 2;
 		int y = bounds.y + (bounds.height - rect.height) / 2;
 		loginShell.setLocation(x, y);
 
 		makeProgressBar();
 		loginShell.open();
 	}
 
 	private Button makeCancelButton(Display display) {
 		final Image cancelImg = new Image(loginShell.getDisplay(),
 				"images/login/cancel.png");
 		Button cancel = new Button(loginShell, SWT.PUSH);
 		cancel.setText("Cancel");
 		cancel.setImage(cancelImg);
 		cancel.pack();
 		cancel.setLocation(400, 250);
 		addHighlightListener(display, cancel);
 		return cancel;
 
 	}
 
 	private Button makeLoginButton(final Display display) {
 		Button login = new Button(loginShell, SWT.PUSH);
 		final Image accept = new Image(loginShell.getDisplay(),
 				"images/login/accept.png");
 		login.setImage(accept);
 		login.setText("Login");
 		login.pack();
 		login.setLocation(300, 250);
 		addHighlightListener(display, login);
 		return login;
 	}
 	
 	private Label makeResultLabel(Display display) {
 		accept = new Image(display,"images/login/shield_accepted.png");
 		deny = new Image(display,"images/login/shield_denied.png");
 		Label result = new Label(this.loginShell,SWT.NONE);
 		result.setBounds(500, 162, 75, 75);
 		result.setImage(accept);
 		result.setVisible(false);
 		return result;
 	}
 	
 	private void makeProgressBar() {
 		bar = new ProgressBar(loginShell, SWT.SMOOTH);
 		bar.setBounds(50, 305, 500, 20);
 		bar.setToolTipText("Test");
 		bar.setVisible(false);
 		textUpdater = new TextUpdater("",bar);
 		bar.addPaintListener(textUpdater);
 	}
 
 	private void addHighlightListener(Display display, Button button) {
 		Color init = new org.eclipse.swt.graphics.Color(display, 3, 3, 3);
 		Color last = new org.eclipse.swt.graphics.Color(display, 105, 105, 105);
 
 		button.setForeground(init);
 
 		final Timeline rolloverTimeline = new Timeline(button);
 		rolloverTimeline.addPropertyToInterpolate("foreground", init, last);
 		rolloverTimeline.setDuration(100);
 
 		button.addMouseTrackListener(new MouseTrackListener() {
 
 			@Override
 			public void mouseEnter(MouseEvent arg0) {
 				rolloverTimeline.play();
 
 			}
 
 			@Override
 			public void mouseExit(MouseEvent arg0) {
 				rolloverTimeline.playReverse();
 
 			}
 
 			@Override
 			public void mouseHover(MouseEvent arg0) {
 			}
 		});
 	}
 
 	public void run(final Display display) {
 		while (!loginShell.isDisposed()) {
			resultLabel.setVisible(visible);
 			if (!display.readAndDispatch())
 				display.sleep();
 		}
 
 		display.dispose();
 	}
 
 	private void updateResult(String message) {
 		this.loginShell.update();
 	}
 
 	private void setSuccessLabel(boolean success) {
 		if (success)
 			resultLabel.setImage(accept);
 		else
 			resultLabel.setImage(deny);
		visible = true;
 		resultLabel.setVisible(true);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				visible = false;
			}
		}).start();
 	}
 	
 	public void dispose() {
 		loginShell.dispose();
 	}
 
 	public void addLoginSuccessListener(Listener listener) {
 		loginSucces.add(listener);
 	}
 
 	public void handleLoginSuccess() {
 		for (Listener l : loginSucces)
 			l.handleEvent(new Event());
 	}
 
 	public void setText(String text) {
 		textUpdater.setText(text);
 	}
 
 	public void updateProgressBar(int amount) {
 		if (!bar.getVisible())
 			bar.setVisible(true);
 		bar.setSelection(bar.getSelection() + amount);
 	}
 
 	public void finishProgressBar() {
 		bar.setSelection(bar.getMaximum());
 	}
 
 	static int[] circle(int r, int offsetX, int offsetY) {
 		int[] polygon = new int[8 * r + 4];
 		// x^2 + y^2 = r^2
 		for (int i = 0; i < 2 * r + 1; i++) {
 			int x = i - r;
 			int y = (int) Math.sqrt(r * r - x * x);
 			polygon[2 * i] = offsetX + x;
 			polygon[2 * i + 1] = offsetY + y;
 			polygon[8 * r - 2 * i - 2] = offsetX + x;
 			polygon[8 * r - 2 * i - 1] = offsetY - y;
 		}
 		return polygon;
 	}
 
 	private class HoverListener implements MouseListener {
 		private Text text;
 
 		public HoverListener ( Text text ) {
 			this.text = text;
 		}
 
 		@Override
 		public void mouseDoubleClick(MouseEvent e) { 
 		}
 
 		@Override
 		public void mouseDown(MouseEvent e) {
 			text.setText("");
 		}
 
 		@Override
 		public void mouseUp(MouseEvent e) {
 		}
 
 	}
 
 	private class TextUpdater implements PaintListener {
 		private String text;
 		private Point point;
 
 		public TextUpdater(String text,ProgressBar pb) {
 			this.text = text;
 			point = pb.getSize();
 		}
 
 		public void setText (String text) {
 			this.text = text;
 		}
 
 		@Override
 		public void paintControl(PaintEvent e) {
 			FontMetrics fontMetrics = e.gc.getFontMetrics();
 			int width = fontMetrics.getAverageCharWidth() * text.length();
 			int height = fontMetrics.getHeight();
 			e.gc.setForeground(loginShell.getDisplay().getSystemColor(SWT.COLOR_BLACK));
 			e.gc.drawString(text, (point.x-width)/2 , (point.y-height)/2, true);
 		}
 	}
 
 }
