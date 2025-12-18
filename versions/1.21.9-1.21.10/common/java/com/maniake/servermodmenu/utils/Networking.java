package com.maniake.servermodmenu.utils;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.maniake.servermodmenu.Constants;
//import net.fabricmc.loader.api.FabricLoader;
//import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.concurrent.atomic.AtomicBoolean;
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
	public Map<String, Long> downloadPercentages = new HashMap<>();
	public Map<String, String> networkErrors = new HashMap<>();
	// Global executor for short tasks
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	// Default request timeout
	private static final long DEFAULT_REQUEST_TIMEOUT_MS = 25000;


    public Networking() {
		// nothing
	}

    public void setupDownloadConnection(String ip){
        ip = GetIPData(ip);

        String key = normalizeIp(ip);
        ServerConnection conn = connections.get(key);
        if (conn == null) {
            return;
        }
        if(conn.pool == null) {
            conn.pool = new DownloadConnectionPool(conn.ip, conn.port, 8);
        }
    }

	/**
	 * Connect (or reuse) a persistent connection to ip:port.
	 */
	public void connect(String ip, int port) {
		if(port == 0) port = 27752;
		// if its a dns address like website.com, get the ip
		ip = GetIPData(ip);

		String key = normalizeIp(ip);
		ServerConnection conn = connections.get(key);
		if (conn != null && conn.isHealthy()) {
			LOGGER.info("Already connected to {}", key);
			return;
		} else {
            if(conn != null){
                Constants.smmServerCount--;
                conn.close();
                connections.remove(key);
            }
        }

		// Try to create a new connection
		try {
			Socket socket = new Socket();
			socket.setTcpNoDelay(true);
			socket.connect(new InetSocketAddress(key, port), 3000);

			connections.put(key, new ServerConnection(key, port, socket, downloadPercentages));
            Constants.smmServerCount++;
            connections.get(key).start();
			LOGGER.info("Connected to {}:{}", key, port);
		} catch (IOException e) {
			LOGGER.error("Could not connect to {}:{} - {}", key, port, e.getMessage());
		}
	}

	private static final Map<String, String> ipCache = new ConcurrentHashMap<>();

	public static String GetIPData(String ipD) {
		// If cached, return instantly
		String cached = ipCache.get(ipD);
		if (cached != null) {
			return cached;
		}

		String ip = "";
		ServerAddress parsedAd = ServerAddress.parseString(ipD);
		Optional<ServerAddress> optAddress =
			AllowedAddressResolver.DEFAULT.resolve(parsedAd);

		if (optAddress.isPresent()) {
			ip = optAddress.get().getHost();
		}

		// Save result in cache so next calls are free
		ipCache.put(ipD, ip);
		return ip;
	}

	public boolean isSocketValid(String ip) {
		ip = GetIPData(ip);

		String key = normalizeIp(ip);
		ServerConnection c = connections.get(key);
		return c != null && c.isHealthy();
	}

	public boolean hasConnectedBefore(String ip) {
		ip = GetIPData(ip);

		String key = normalizeIp(ip);
		ServerConnection c = connections.get(key);
		return c != null;
	}

	/**
	 * Fire-and-forget send. Non-blocking. If connection does not exist, returns false.
	 */
	public boolean send(String ip, String data) {
		ip = GetIPData(ip);
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
		ip = GetIPData(ip);
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
	public boolean downloadFile(String ip, String modId, String filename, Path dest, long timeoutMs) {
		ip = GetIPData(ip);
		ServerConnection c = connections.get(normalizeIp(ip));
		if (c == null || !c.isHealthy()) return false;
		try {
			return c.downloadFile(modId, filename, dest, timeoutMs);
		} catch (IOException | InterruptedException e) {
			LOGGER.error("Download failed for {}:{} -> {}", ip, filename, e.getMessage());
			return false;
		}
	}

	public void clearAll(String ip){
		ip = GetIPData(ip);
		ServerConnection c = connections.get(normalizeIp(ip));
		if (c == null || !c.isHealthy()) return;
		c.clearAll();
	}

	public boolean downloadFile(String id, String ip, String filename, Path dest) {
		return downloadFile(id, ip, filename, dest, TimeUnit.SECONDS.toMillis(60));
	}

	/**
	 * Close and remove connection
	 */
	public void disconnect(String ip) {
        Constants.smmInstalledModCount.remove(ip);
        Constants.smmUninstalledModCount.remove(ip);
        Constants.smmOutofDateModCount.remove(ip);
        Constants.smmModCount.remove(ip);
		ip = GetIPData(ip);
		String key = normalizeIp(ip);
		ServerConnection sc = connections.remove(key);
		if (sc != null) sc.close();
        Constants.smmServerCount--;
	}

	public void shutdown() {
        Constants.smmInstalledModCount.clear();
        Constants.smmUninstalledModCount.clear();
        Constants.smmOutofDateModCount.clear();
        Constants.smmServerCount = 0;
        Constants.smmModCount.clear();
		connections.values().forEach(ServerConnection::close);
		connections.clear();
		scheduler.shutdownNow();
	}

	private static String normalizeIp(String ip) {
		if (ip == null) return "";
		if (ip.contains(":")) return ip.substring(0, ip.indexOf(":"));
		return ip;
	}

	// ----------------------
	// Per-connection state
	// ----------------------
	private static class ServerConnection {
		final String ip;
		final int port;
		Socket socket;
		volatile boolean running = false;
		public Map<String, Long> downloadPercentages = null;
        DownloadConnectionPool pool = null;

		final BlockingQueue<String> outgoing = new LinkedBlockingQueue<>();
		final BlockingQueue<String> incoming = new LinkedBlockingQueue<>();

		// Threads
		Thread senderThread;
		Thread listenerThread;

		// Handler for unsolicited messages
		volatile BiConsumer<String, String> messageHandler = null;



		ServerConnection(String ip, int port, Socket socket, Map<String, Long> downloadPercentages) throws IOException {
			this.downloadPercentages = downloadPercentages;
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
			pauseListener.set(false);
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
//			incomingRaw.clear();
			lineBuffer.reset();
		}

		String pollIncoming(long timeout, TimeUnit unit) throws InterruptedException {
//			incomingRaw.poll(timeout, unit);
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
		AtomicBoolean pauseListener = new AtomicBoolean(false);

		ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
		void runListener() {
			try {
				InputStream in = socket.getInputStream();
				byte[] buf = new byte[8192];
				int read;

				while (running && !socket.isClosed()) {
					if(pauseListener.get()) continue;
					if ((read = in.read(buf)) != -1) {
//						LOGGER.info("READING DATA");
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
		boolean downloadFile(String modId, String filename, Path dest, long timeoutMs) throws IOException, InterruptedException {
			try {
				pauseListener.set(true);
                Socket dSocket = pool.acquire();
				PrintWriter out = new PrintWriter(dSocket.getOutputStream(), true);
				InputStream in = dSocket.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
				out.println("download|" + filename);
				out.flush();

				String header = reader.readLine();//pollIncoming(timeoutMs, TimeUnit.MICROSECONDS); // reads until '\n', strips it

				if (!header.startsWith("filesize|")) {
					throw new IOException("Invalid download header: " + header);
				}

				long fileSize = Long.parseLong(header.split("\\|")[1]);

				PrintWriter writer = new PrintWriter(dSocket.getOutputStream(), true);
				writer.println("OKAY");
				writer.flush();

				try (FileOutputStream fos = new FileOutputStream(dest.toFile())) {
					byte[] buf = new byte[8192];
					int read = 0;
					long downloaded = 0;
					long lastPercent = -1;

					while (downloaded < fileSize) {
						int read2 = in.read(buf);
						if (read2 == -1) {
							throw new IOException("Unexpected EOF before receiving full file");
						}

						fos.write(buf, 0, read2);
						downloaded += read2;

						long percent = (downloaded * 100) / fileSize;
						if (percent != lastPercent) {
							lastPercent = percent;
							LOGGER.info("percentage: {}%", lastPercent);
							downloadPercentages.put(modId+"|"+socket.getInetAddress().getHostName(), lastPercent);
						}
					}
				}

				pauseListener.set(false);

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
            if(!running) return;
			running = false;
			try {
				if (socket != null && !socket.isClosed()) socket.close();
                senderThread.interrupt();
                listenerThread.interrupt();
            } catch (IOException ignored) {
			}
		}
	}

	// ----------------------
	// Helpers used by original codebase
	// ----------------------
	public String requestNResponse(String ip, String data) {
		ip = GetIPData(ip);
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

	public Long getDThreadProgress(String ip, String id){
		ip = GetIPData(ip);
		return downloadPercentages.get(id+"|"+ip);
	}

	// For exclusive operations like raw downloads
	final ReentrantLock exclusiveLock = new ReentrantLock();

	int downloadAttempts = 0;

	// A helper used in the original class to download by id
	public void requestNDownload(String ip, String id, String version) {

		if (Constants.idsDLD.contains(id) || isModAlreadyPresent(id, version)) {
            Constants.idsDLD.add(id);
			LOGGER.info("Mod {} already present", id);
			return;
		}

        Constants.ExternalModManager.DeleteExistingMod(id);

		// Run download in background thread
		Thread downloadThread = new Thread(() -> {
			try {
				if(Objects.equals(id, "forge") || Objects.equals(id, "neoforge") || Objects.equals(id, "fabric") || id.contains("fabric-api") || id.contains("fabricloader") || id.contains("minecraft")) return;
				int port = 27752; // default, change if you store ports
                connect(ip, 27752);
                setupDownloadConnection(ip);
                Socket bla = connections.get(ip).pool.acquire();
                PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(bla.getOutputStream(), StandardCharsets.UTF_8),
                        true
                );
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(bla.getInputStream(), StandardCharsets.UTF_8)
                );
                out.println("getmod|" + id);
                String fileN = in.readLine();
                // if null, the socket might be dead, we'll close and try doing this once again but if that doesn't work, the server or mod is invalid.
                bla.close();

				if (fileN == null) {
					LOGGER.error("Mod {} does not exist or skipped.", id);
					networkErrors.put(ip+id, "ERR");
//					exclusiveLock.unlock();
					return;
				}

				Path modsFolder = Paths.get("./mods");
				if (!Files.exists(modsFolder)) Files.createDirectories(modsFolder);
				Path modFile = modsFolder.resolve(fileN);

				boolean ok = downloadFile(ip, id, fileN, modFile, TimeUnit.SECONDS.toMillis(DEFAULT_REQUEST_TIMEOUT_MS));
				while (!ok){
					if(downloadAttempts <= 3){
						disconnect(ip);
						connect(ip, port);
						networkErrors.remove(ip+id);
						downloadAttempts++;
						ok = downloadFile(ip, id, fileN, modFile, TimeUnit.SECONDS.toMillis(DEFAULT_REQUEST_TIMEOUT_MS));
					} else {
						break;
					}
				}

				if (!ok) {
					LOGGER.error("Failed to download {}", fileN);
					networkErrors.put(ip+id, "ERR");

//						exclusiveLock.unlock();

					return;
				}

				downloadAttempts = 0;

				// Check deps
				checkAndDownloadDependencies(ip, modFile);

                Constants.idsDLD.add(id);
				boolean allHidden = Constants.buttonEntries.values().stream().allMatch(b -> !b.visible);
				if (allHidden) Constants.isAllDFB = true;
				LOGGER.info("Mod downloaded successfully: {}", fileN);
				networkErrors.put(ip+id, "OK");
			} catch (Exception e) {
				LOGGER.error("Failed to download mod {}: {}", id, e.getMessage(), e);
				networkErrors.put(ip+id, "ERR");
//				exclusiveLock.unlock();
			}

			downloadThreads.remove(ip + id);
//			exclusiveLock.unlock();
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
								JsonElement versionElement = dependencies.get(dep);
								String depVersion;

								if (versionElement.isJsonArray()) {
									// Handle arrays like ["<2.0.0", ">=1.1.0"]
									// Concatenate them into a single string for logging/comparison
									StringBuilder versionBuilder = new StringBuilder();
									for (JsonElement element : versionElement.getAsJsonArray()) {
										if (!versionBuilder.isEmpty()) versionBuilder.append(", ");
										versionBuilder.append(element.getAsString());
									}
									depVersion = versionBuilder.toString();
								} else {
									// Handle single strings (e.g., "1.0.0")
									depVersion = versionElement.getAsString();
								}

								LOGGER.info("Dependency found: {} version: {}", dep, depVersion);
								if (!isModAlreadyPresent(dep, depVersion) && !Objects.equals(dep, "fabricloader")) {
									requestNDownload(ip, dep, depVersion);
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

        try {
            td.join(); // <- waits until fully finished
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
	}

	public static boolean isModAlreadyPresent(String modName, String version) {
//		Optional<ModContainer> modContainerOptional = FabricLoader.getInstance().getModContainer(modName);
		if (!Constants.ExternalModManager.DoesModExist(modName)) {
			return Constants.idsDLD.contains(modName);
		} else {
			if(Objects.equals(modName, "minecraft") || Objects.equals(modName, "java")) return true; // smm will check if its the right version mid init

            //modContainerOptional.get().getMetadata().getVersion().getFriendlyString()
			if(!Objects.equals(Constants.ExternalModManager.GetModVersion(modName), version)){
				return Objects.equals(version, "*");
			}
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

		public Optional<ServerAddress> resolve(ServerAddress address) {
			Optional<ServerAddress> optional = this.addressResolver.resolve(address);
			if (optional.isPresent()){
				Optional<ServerAddress> optional2 = this.redirectResolver.lookupRedirect(address);
				if (optional2.isPresent()) {
					optional = this.addressResolver.resolve(optional2.get());
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
		AddressResolver DEFAULT = (address) -> {
            return Optional.of(new ServerAddress(address.getHost(), 27752));
        };

		Optional<ServerAddress> resolve(ServerAddress address);
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
					try {
						Attributes attributes = dirContext.getAttributes("_scmc._tcp." + address.getHost(), new String[]{"SRV"});
						Attribute attribute = attributes.get("srv");
						if (attribute != null) {
							String[] strings = attribute.get().toString().split(" ", 4);
							return Optional.of(new ServerAddress(strings[3], Networking.portOrDefault(strings[2])));
						}
					} catch (Throwable var5) {
					}

				return Optional.empty();
			};
		}
	}
//
//	public static final class ModServerAddress {
//		private static final Logger LOGGER = LogUtils.getLogger();
//		private final HostAndPort hostAndPort;
//		private static final ModServerAddress INVALID = new ModServerAddress(HostAndPort.fromParts("server.invalid", 25565));
//
//		public ModServerAddress(String host, int port) {
//			this(HostAndPort.fromParts(host, port));
//		}
//
//		private ModServerAddress(HostAndPort hostAndPort) {
//			this.hostAndPort = hostAndPort;
//		}
//
//		public String getAddress() {
//			try {
//				return IDN.toASCII(this.hostAndPort.getHost());
//			} catch (IllegalArgumentException var2) {
//				return "";
//			}
//		}
//
//		public int getPort() {
//			return this.hostAndPort.getPort();
//		}
//
//		public static ModServerAddress parse(String address) {
//			if (address == null) {
//				return INVALID;
//			} else {
//				try {
//					HostAndPort hostAndPort = HostAndPort.fromString(address).withDefaultPort(27752);
//					return hostAndPort.getHost().isEmpty() ? INVALID : new ModServerAddress(hostAndPort);
//				} catch (IllegalArgumentException var2) {
//					IllegalArgumentException illegalArgumentException = var2;
//					LOGGER.info("Failed to parse URL {}", address, illegalArgumentException);
//					return INVALID;
//				}
//			}
//		}
//
//		public static boolean isValid(String address) {
//			try {
//				HostAndPort hostAndPort = HostAndPort.fromString(address);
//				String string = hostAndPort.getHost();
//				if (!string.isEmpty()) {
//					IDN.toASCII(string);
//					return true;
//				}
//			} catch (IllegalArgumentException var3) {
//			}
//
//			return false;
//		}
//
//		static int portOrDefault(String port) {
//			try {
//				return Integer.parseInt(port.trim());
//			} catch (Exception var2) {
//				return 27752;
//			}
//		}
//
//		public String toString() {
//			return this.hostAndPort.toString();
//		}
//
//		public boolean equals(Object o) {
//			if (this == o) {
//				return true;
//			} else {
//                if (o instanceof ServerAddress) {
//                    ((AccessorServerAddress) o).getHostAndPort();
//                }
//                return false;
//			}
//		}
//
//		public int hashCode() {
//			return this.hostAndPort.hashCode();
//		}
//	}

    public class DownloadConnectionPool {
        private final String ip;
        private final int port;

        private final BlockingQueue<Socket> pool = new LinkedBlockingQueue<>();
        private final int maxConnections;

        public DownloadConnectionPool(String ip, int port, int maxConnections) {
            this.ip = ip;
            this.port = port;
            this.maxConnections = maxConnections;
        }

        public Socket acquire() throws IOException {
            Socket sock = pool.poll();
            if (sock != null && sock.isConnected() && !sock.isClosed()) {
                return sock; // reuse
            }

            // otherwise create new
            return new Socket(ip, port);
        }

        public void release(Socket sock) {
            if (sock == null) return;
            if (pool.size() < maxConnections) {
                pool.offer(sock);
            } else {
                try { sock.close(); } catch (Exception ignored) {}
            }
        }

        public void shutdown() {
            for (Socket s : pool) {
                try { s.close(); } catch (Exception ignored) {}
            }
            pool.clear();
        }
    }



}
