package com.skellybuilds.servermodmenu.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.skellybuilds.servermodmenu.ModMenu;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.HostAndPort;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

// Rewritten to be less terrible with threading.
public class Networking {
	public static final Logger LOGGER = LoggerFactory.getLogger("Server Mod Menu");

	private final ConcurrentMap<String, ServerConnection> connections = new ConcurrentHashMap<>();
	public Map<String, Thread> downloadThreads = new HashMap<>();
	public Map<String, String> networkErrors = new HashMap<>();
	// Global executor for short tasks
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	// Default request timeout
	private static final long DEFAULT_REQUEST_TIMEOUT_MS = 5000;

	public Networking() {
		// nothing
	}

	/**
	 * Connect (or reuse) a persistent connection to ip:port.
	 */
	public void connect(String ip, int port) {
		String key = normalizeIp(ip);
		ServerConnection conn = connections.get(key);
		if (conn != null && conn.isHealthy()) {
			LOGGER.info("Already connected to {}", key);
			return;
		}

		// Try to create a new connection
		try {
			Socket socket = new Socket();
			socket.setTcpNoDelay(true);
			socket.connect(new InetSocketAddress(key, port), 3000);

			ServerConnection sc = new ServerConnection(key, port, socket);
			ServerConnection prev = connections.put(key, sc);
			if (prev != null) {
				prev.close();
			}

			sc.start();
			LOGGER.info("Connected to {}:{}", key, port);
		} catch (IOException e) {
			LOGGER.error("Could not connect to {}:{} - {}", key, port, e.getMessage());
		}
	}

	public boolean isSocketValid(String ip) {
		String key = normalizeIp(ip);
		ServerConnection c = connections.get(key);
		return c != null && c.isHealthy();
	}

	/**
	 * Fire-and-forget send. Non-blocking. If connection does not exist, returns false.
	 */
	public boolean send(String ip, String data) {
		ServerConnection c = connections.get(normalizeIp(ip));
		if (c == null || !c.isHealthy()) return false;
		c.enqueueOutgoing(data);
		try {
			c.pollIncoming(DEFAULT_REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
		return true;
	}

	/**
	 * Send and wait for a single-line response. Returns null on timeout/error.
	 * This method assumes server replies in-order; the listener will enqueue incoming lines and this method will poll
	 * the incoming queue for the next available line.
	 */
	public String request(String ip, String data, long timeoutMs) {
		ServerConnection c = connections.get(normalizeIp(ip));
		if (c == null || !c.isHealthy()) return null;
		try {
			c.enqueueOutgoing(data);
			return c.pollIncoming(timeoutMs, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	public String request(String ip, String data) {
		return request(ip, data, DEFAULT_REQUEST_TIMEOUT_MS);
	}

	/**
	 * Register a message handler that will receive unsolicited messages from the server.
	 * Handler receives (ip, message).
	 */
	public void onMessage(BiConsumer<String, String> handler) {
		// Broadcast handler to all connections
		connections.values().forEach(conn -> conn.setMessageHandler(handler));
	}

	/**
	 * Download a file from the server. This method will temporarily pause the normal listener for this connection
	 * and read raw bytes from the socket input stream, writing them to 'dest'.
	 *
	 * WARNING: This is protocol-sensitive: server must begin streaming raw file bytes after the "download|filename" command.
	 */
	public boolean downloadFile(String ip, String filename, Path dest, long timeoutMs) {
		ServerConnection c = connections.get(normalizeIp(ip));
		if (c == null || !c.isHealthy()) return false;
		try {
			return c.downloadFile(filename, dest, timeoutMs);
		} catch (IOException | InterruptedException e) {
			LOGGER.error("Download failed for {}:{} -> {}", ip, filename, e.getMessage());
			return false;
		}
	}

	public void clearAll(String ip){
		ServerConnection c = connections.get(normalizeIp(ip));
		if (c == null || !c.isHealthy()) return;
		c.clearAll();
	}

	public boolean downloadFile(String ip, String filename, Path dest) {
		return downloadFile(ip, filename, dest, TimeUnit.SECONDS.toMillis(60));
	}

	/**
	 * Close and remove connection
	 */
	public void disconnect(String ip) {
		String key = normalizeIp(ip);
		ServerConnection sc = connections.remove(key);
		if (sc != null) sc.close();
	}

	public void shutdown() {
		connections.values().forEach(ServerConnection::close);
		connections.clear();
		scheduler.shutdownNow();
	}

	private static String normalizeIp(String ip) {
		if (ip == null) return "";
		if (ip.contains(":")) return ip.substring(0, ip.indexOf(":"));
		return ip;
	}

	private static class DownloadHandler {
		private final Path dest;
		private final long fileSize;
		private final CountDownLatch done = new CountDownLatch(1);

		DownloadHandler(Path dest, long fileSize) {
			this.dest = dest;
			this.fileSize = fileSize;
		}

		void consume(InputStream raw) throws IOException {
			try (FileOutputStream fos = new FileOutputStream(dest.toFile())) {
				byte[] buf = new byte[8192];
				long remaining = fileSize;

				while (remaining > 0) {
					int n = raw.read(buf, 0, (int)Math.min(buf.length, remaining));
					if (n < 0) throw new IOException("Unexpected EOF during download");
					fos.write(buf, 0, n);
					remaining -= n;
				}
			} finally {
				done.countDown();
			}
		}

		void awaitDone(long timeoutMs) throws InterruptedException {
			if (!done.await(timeoutMs, TimeUnit.MILLISECONDS)) {
				throw new InterruptedException("Download timeout");
			}
		}
	}

	// ----------------------
	// Per-connection state
	// ----------------------
	private static class ServerConnection {
		final String ip;
		final int port;
		Socket socket;
		volatile boolean running = false;

		final BlockingQueue<String> outgoing = new LinkedBlockingQueue<>();
		final BlockingQueue<String> incoming = new LinkedBlockingQueue<>();
		final BlockingQueue<byte[]> incomingRaw = new LinkedBlockingQueue<>();
		private final BlockingQueue<DownloadHandler> downloadQueue = new LinkedBlockingQueue<>();
		private final ConcurrentHashMap<String, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();
		private final AtomicLong requestCounter = new AtomicLong(0);

		// Threads
		Thread senderThread;
		Thread listenerThread;

		// Handler for unsolicited messages
		volatile BiConsumer<String, String> messageHandler = null;



		ServerConnection(String ip, int port, Socket socket) throws IOException {
			this.ip = ip;
			this.port = port;
			this.socket = socket;
		}

		boolean isHealthy() {
			return socket != null && socket.isConnected() && !socket.isClosed();
		}

		void start() throws IOException {
			if (!isHealthy()) throw new IOException("Socket not connected");
			running = true;

			// sender thread
			senderThread = new Thread(this::runSender, "[Net-Sender] " + ip);
			senderThread.setDaemon(true);
			senderThread.start();

			// listener thread
			listenerThread = new Thread(this::runListener, "[Net-Listener] " + ip);
			listenerThread.setDaemon(true);
			listenerThread.start();
		}

		void setMessageHandler(BiConsumer<String, String> handler) {
			this.messageHandler = handler;
		}

		void enqueueOutgoing(String s) {
			outgoing.offer(s);
		}

		void clearAll(){
			incoming.clear();
			incomingRaw.clear();
			lineBuffer.reset();
		}

		String pollIncoming(long timeout, TimeUnit unit) throws InterruptedException {
			incomingRaw.poll(timeout, unit);
			String data = incoming.poll(timeout, unit);
			return data == null ? "TIMEOUT" : data;
		}

		void runSender() {
			try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
				while (running && !socket.isClosed()) {
					try {
						String msg = outgoing.take(); // blocks
//							LOGGER.info(msg);
						out.println(msg);
						out.flush();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			} catch (IOException e) {
				LOGGER.error("Sender thread for {} stopped: {}", ip, e.getMessage());
			} finally {
				running = false;
			}
		}
		ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
		void runListener() {
			try {
				InputStream in = socket.getInputStream();
				byte[] buf = new byte[8192];
				int read;

				while (running && (read = in.read(buf)) != -1) {
					// Offer the raw bytes
					byte[] rawChunk = Arrays.copyOf(buf, read);
					incomingRaw.offer(rawChunk);

					// Process lines for commands/messages
					for (int i = 0; i < read; i++) {
						byte b = buf[i];

						if (b == '\n') {
							String line = lineBuffer.toString(StandardCharsets.UTF_8);
							if (line.endsWith("\r")) {
								line = line.substring(0, line.length() - 1);
							}
							incoming.offer(line);
							if (messageHandler != null) messageHandler.accept(ip, line);
							lineBuffer.reset();
						} else {
							lineBuffer.write(b);
						}
					}
				}
			} catch (IOException e) {
				LOGGER.error("Listener thread for {} stopped: {}", ip, e.getMessage());
				try {
					socket.close();
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			} finally {
				running = false;
			}
		}



		/**
		 * Download file by pausing listener and reading raw bytes into 'dest'.
		 */
		boolean downloadFile(String filename, Path dest, long timeoutMs) throws IOException, InterruptedException {


			try {
				// Request download header via queue system
				String header;
				enqueueOutgoing("download|" + filename);
				header = pollIncoming(timeoutMs, TimeUnit.MILLISECONDS);
				if (header == null || !header.startsWith("filesize|")) {
					throw new IOException("Invalid download header: " + header);
				}

				long fileSize = Long.parseLong(header.split("\\|")[1]);
				incomingRaw.clear();

				try (FileOutputStream fos = new FileOutputStream(dest.toFile())) {
//						byte[] buf = new byte[8192];
					long remaining = fileSize;

					while (remaining > 0) {
						byte[] data = incomingRaw.poll(timeoutMs, TimeUnit.MILLISECONDS);
						if (data == null) throw new IOException("Unexpected EOF during download");
						fos.write(data, 0, data.length);
						remaining -= data.length;
					}
				}

				incoming.clear();
				incomingRaw.clear();

				// Optionally validate ZIP
				try (ZipFile z = new ZipFile(dest.toFile())) {
					// OK
				} catch (ZipException ze) {
					LOGGER.error("Downloaded file is not a valid zip: {}", ze.getMessage());
					Files.deleteIfExists(dest);
					return false;
				}

				return true;
			} finally {

			}
		}

		void close() {
			running = false;
			try {
				if (socket != null && !socket.isClosed()) socket.close();
			} catch (IOException ignored) {
			}
		}
	}

	// ----------------------
	// Helpers used by original codebase
	// ----------------------
	public String requestNResponse(String ip, String data) {
		String res = request(ip, data, DEFAULT_REQUEST_TIMEOUT_MS);
		if (res == null) return "EXCEPTION";
		return res;
	}

	public boolean isDthreadsDone() {
		return !downloadThreads.isEmpty();
	}

	public boolean isDthreadDone(String ip, String id) {
		return downloadThreads.get(ip + id) == null;
	}

	// For exclusive operations like raw downloads
	final ReentrantLock exclusiveLock = new ReentrantLock();

	// A helper used in the original class to download by id
	public void requestNDownload(String ip, String id) {


		if (ModMenu.idsDLD.contains(id) || isModAlreadyPresent(id)) {
			LOGGER.info("Mod {} already present", id);
			return;
		}

		// Run download in background thread
		Thread downloadThread = new Thread(() -> {
			try {
				if(id.contains("fabric-api") || id.contains("fabricloader") || id.contains("minecraft")) return;

				while(!exclusiveLock.tryLock()) {

				}
				// ensure connection
				int port = 27752; // default, change if you store ports
				connect(ip, port);
				clearAll(ip);
				String fileN = requestNResponse(ip, "getmod|" + id);
				if (fileN == null) {
					LOGGER.error("Mod {} does not exist or skipped.", id);
					networkErrors.put(ip+id, "ERR");
					return;
				}

				Path modsFolder = Paths.get("./mods");
				if (!Files.exists(modsFolder)) Files.createDirectories(modsFolder);
				Path modFile = modsFolder.resolve(fileN);

				boolean ok = downloadFile(ip, fileN, modFile, TimeUnit.SECONDS.toMillis(60));
				if (!ok) {
					LOGGER.error("Failed to download {}", fileN);
					networkErrors.put(ip+id, "ERR");
					return;
				}

				// Check deps
				checkAndDownloadDependencies(ip, modFile);

				ModMenu.idsDLD.add(id);
				boolean allHidden = ModMenu.buttonEntries.values().stream().allMatch(b -> !b.visible);
				if (allHidden) ModMenu.isAllDFB = true;
				LOGGER.info("Mod downloaded successfully: {}", fileN);
				networkErrors.put(ip+id, "OK");
			} catch (Exception e) {
				LOGGER.error("Failed to download mod {}: {}", id, e.getMessage(), e);
				networkErrors.put(ip+id, "ERR");
			}



			downloadThreads.remove(ip + id);
			exclusiveLock.unlock();
		}, "[ServerModMenu] Download Manager - " + ip +" " + id);

		downloadThreads.put(ip + id, downloadThread);
		downloadThread.setDaemon(true);
		downloadThread.start();


	}

	// Reuse the original dependency logic (kept mostly intact, but runs in background)
	private void checkAndDownloadDependencies(String ip, Path modFilePath) {
		Thread td = new Thread(() -> {
			try (JarFile jarFile = new JarFile(modFilePath.toFile())) {
				ZipEntry entry = jarFile.getEntry("fabric.mod.json");
				if (entry != null) {
					try (InputStream inputStream = jarFile.getInputStream(entry)) {
						JsonObject jsonObject = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();
						if (jsonObject.has("depends")) {
							JsonObject dependencies = jsonObject.getAsJsonObject("depends");

							for (String dep : dependencies.keySet()) {
								String depVersion = dependencies.get(dep).getAsString();
								LOGGER.info("Dependency found: {} version: {}", dep, depVersion);
								if (!isModAlreadyPresent(dep) && !Objects.equals(dep, "fabricloader")) {
									requestNDownload(ip, dep);
								}
							}
						}
					}
				}
			} catch (IOException e) {
				LOGGER.error("Failed to read mod dependencies: {}", e.getMessage());
			}
		});
		td.setDaemon(true);
		td.start();

		// wait for thread to finish similarly to original code
		while (td.getState() == Thread.State.RUNNABLE) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	public static boolean isModAlreadyPresent(String modName) {
		Optional<ModContainer> modContainerOptional = FabricLoader.getInstance().getModContainer(modName);
		if (modContainerOptional.isEmpty()) {
			return ModMenu.idsDLD.contains(modName);
		} else {
			return true;
		}
	}

	static int portOrDefault(String port) {
		try {
			return Integer.parseInt(port.trim());
		} catch (Exception var2) {
			return 27752;
		}
	}

	public static class AllowedAddressResolver {
		public static final AllowedAddressResolver DEFAULT;
		private final AddressResolver addressResolver;
		private final RedirectResolver redirectResolver;
		//private final BlockListChecker blockListChecker;

		@VisibleForTesting
		AllowedAddressResolver(AddressResolver addressResolver, RedirectResolver redirectResolver) {
			this.addressResolver = addressResolver;
			this.redirectResolver = redirectResolver;
		}

		public Optional<Address> resolve(ServerAddress address) {
			Optional<Address> optional = this.addressResolver.resolve(address);
			if (optional.isPresent()){
				Optional<ServerAddress> optional2 = this.redirectResolver.lookupRedirect(address);
				if (optional2.isPresent()) {
					optional = this.addressResolver.resolve((ServerAddress)optional2.get());
				}

				return optional;
			} else {
				return Optional.empty();
			}
		}

		static {
			DEFAULT = new AllowedAddressResolver(AddressResolver.DEFAULT, RedirectResolver.createSrv());
		}
	}


	public interface AddressResolver {
		Logger LOGGER = LogUtils.getLogger();
		AddressResolver DEFAULT = (address) -> {
			try {
				InetAddress inetAddress = InetAddress.getByName(address.getAddress());
				return Optional.of(Address.create(new InetSocketAddress(inetAddress, address.getPort())));
			} catch (UnknownHostException var2) {
				UnknownHostException unknownHostException = var2;
				LOGGER.debug("Couldn't resolve server {} address", address.getAddress(), unknownHostException);
				return Optional.empty();
			}
		};

		Optional<Address> resolve(ServerAddress address);
	}


	public interface RedirectResolver {
		Logger LOGGER = LogUtils.getLogger();
		RedirectResolver INVALID = (address) -> {
			return Optional.empty();
		};

		Optional<ServerAddress> lookupRedirect(ServerAddress address);

		static RedirectResolver createSrv() {
			InitialDirContext dirContext;
			try {
				String string = "com.sun.jndi.dns.DnsContextFactory";
				Class.forName("com.sun.jndi.dns.DnsContextFactory");
				Hashtable<String, String> hashtable = new Hashtable<>(); // Why mojang?
				hashtable.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
				hashtable.put("java.naming.provider.url", "dns:");
				hashtable.put("com.sun.jndi.dns.timeout.retries", "1");
				dirContext = new InitialDirContext(hashtable);
			} catch (Throwable var3) {
				Throwable throwable = var3;
				LOGGER.error("Failed to initialize SRV redirect resolved, some servers might not work", throwable);
				return INVALID;
			}

			return (address) -> {
				if (address.getPort() == 27752) {
					try {
						Attributes attributes = dirContext.getAttributes("_scmc._tcp." + address.getAddress(), new String[]{"SRV"});
						Attribute attribute = attributes.get("srv");
						if (attribute != null) {
							String[] strings = attribute.get().toString().split(" ", 4);
							return Optional.of(new ServerAddress(strings[3], Networking.portOrDefault(strings[2])));
						}
					} catch (Throwable var5) {
					}
				}

				return Optional.empty();
			};
		}
	}

	public static final class ServerAddress {
		private static final Logger LOGGER = LogUtils.getLogger();
		private final HostAndPort hostAndPort;
		private static final ServerAddress INVALID = new ServerAddress(HostAndPort.fromParts("server.invalid", 25565));

		public ServerAddress(String host, int port) {
			this(HostAndPort.fromParts(host, port));
		}

		private ServerAddress(HostAndPort hostAndPort) {
			this.hostAndPort = hostAndPort;
		}

		public String getAddress() {
			try {
				return IDN.toASCII(this.hostAndPort.getHost());
			} catch (IllegalArgumentException var2) {
				return "";
			}
		}

		public int getPort() {
			return this.hostAndPort.getPort();
		}

		public static ServerAddress parse(String address) {
			if (address == null) {
				return INVALID;
			} else {
				try {
					HostAndPort hostAndPort = HostAndPort.fromString(address).withDefaultPort(27752);
					return hostAndPort.getHost().isEmpty() ? INVALID : new ServerAddress(hostAndPort);
				} catch (IllegalArgumentException var2) {
					IllegalArgumentException illegalArgumentException = var2;
					LOGGER.info("Failed to parse URL {}", address, illegalArgumentException);
					return INVALID;
				}
			}
		}

		public static boolean isValid(String address) {
			try {
				HostAndPort hostAndPort = HostAndPort.fromString(address);
				String string = hostAndPort.getHost();
				if (!string.isEmpty()) {
					IDN.toASCII(string);
					return true;
				}
			} catch (IllegalArgumentException var3) {
			}

			return false;
		}

		static int portOrDefault(String port) {
			try {
				return Integer.parseInt(port.trim());
			} catch (Exception var2) {
				return 27752;
			}
		}

		public String toString() {
			return this.hostAndPort.toString();
		}

		public boolean equals(Object o) {
			if (this == o) {
				return true;
			} else {
				return o instanceof ServerAddress && this.hostAndPort.equals(((ServerAddress) o).hostAndPort);
			}
		}

		public int hashCode() {
			return this.hostAndPort.hashCode();
		}
	}


}
