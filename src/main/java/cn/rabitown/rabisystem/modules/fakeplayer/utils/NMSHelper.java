package cn.rabitown.rabisystem.modules.fakeplayer.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NMSHelper {

    private static boolean initialized = false;

    // NMS Classes
    private static Class<?> serverPlayerClass;
    private static Class<?> serverLevelClass;
    private static Class<?> minecraftServerClass;
    private static Class<?> gameProfileClass;
    private static Class<?> clientInformationClass;
    private static Class<?> connectionClass;
    private static Class<?> packetFlowClass;
    private static Class<?> packetListenerClass;
    private static Class<?> commonListenerCookieClass;
    private static Class<?> playerListClass;

    // Netty Classes (Reflected)
    private static Class<?> channelClass;
    private static Class<?> channelPipelineClass;
    private static Class<?> channelFutureClass;
    private static Class<?> eventLoopClass;
    private static Class<?> attributeClass;
    private static Class<?> attributeKeyClass;

    // Methods & Fields
    private static Method getHandleWorldMethod;
    private static Method getHandleServerMethod;
    private static Method getPlayerListMethod;
    private static Method createDefaultClientInfoMethod;
    private static Method placeNewPlayerMethod;
    private static Field connectionField;
    private static Field channelField;

    // Objects
    private static Object minecraftServer;

    // Constructors
    private static Constructor<?> serverPlayerConstructor;
    private static Constructor<?> connectionConstructor;
    private static Constructor<?> packetListenerConstructor;
    private static Constructor<?> cookieConstructor;

    public static void init() {
        if (initialized) return;
        try {
            String cbPackage = Bukkit.getServer().getClass().getPackage().getName();

            // 1. Basic Reflection
            Class<?> craftWorldClass = Class.forName(cbPackage + ".CraftWorld");
            getHandleWorldMethod = craftWorldClass.getMethod("getHandle");

            Class<?> craftServerClass = Class.forName(cbPackage + ".CraftServer");
            getHandleServerMethod = craftServerClass.getMethod("getServer");
            minecraftServer = getHandleServerMethod.invoke(Bukkit.getServer());

            // 2. Load NMS Classes
            serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");
            serverLevelClass = Class.forName("net.minecraft.server.level.ServerLevel");
            minecraftServerClass = Class.forName("net.minecraft.server.MinecraftServer");
            gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            clientInformationClass = Class.forName("net.minecraft.server.level.ClientInformation");
            connectionClass = Class.forName("net.minecraft.network.Connection");
            packetFlowClass = Class.forName("net.minecraft.network.protocol.PacketFlow");
            packetListenerClass = Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl");
            commonListenerCookieClass = Class.forName("net.minecraft.server.network.CommonListenerCookie");
            playerListClass = Class.forName("net.minecraft.server.players.PlayerList");

            // Load Netty Classes
            channelClass = Class.forName("io.netty.channel.Channel");
            channelPipelineClass = Class.forName("io.netty.channel.ChannelPipeline");
            channelFutureClass = Class.forName("io.netty.channel.ChannelFuture");
            eventLoopClass = Class.forName("io.netty.channel.EventLoop");
            attributeClass = Class.forName("io.netty.util.Attribute");
            attributeKeyClass = Class.forName("io.netty.util.AttributeKey");

            // 3. Methods & Fields
            createDefaultClientInfoMethod = clientInformationClass.getMethod("createDefault");
            connectionField = serverPlayerClass.getField("connection");

            try {
                channelField = connectionClass.getDeclaredField("channel");
                channelField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("Cannot find 'channel' field in Connection class");
            }

            getPlayerListMethod = minecraftServerClass.getMethod("getPlayerList");

            // 4. Constructors
            serverPlayerConstructor = serverPlayerClass.getConstructor(
                    minecraftServerClass, serverLevelClass, gameProfileClass, clientInformationClass
            );
            connectionConstructor = connectionClass.getConstructor(packetFlowClass);

            // Cookie Finder
            for (Constructor<?> c : commonListenerCookieClass.getConstructors()) {
                if (c.getParameterCount() >= 2 && c.getParameterTypes()[0] == gameProfileClass) {
                    cookieConstructor = c;
                    break;
                }
            }

            // PacketListener Finder
            for (Constructor<?> c : packetListenerClass.getConstructors()) {
                Class<?>[] types = c.getParameterTypes();
                if (types.length >= 3
                        && types[0] == minecraftServerClass
                        && types[1] == connectionClass
                        && types[2] == serverPlayerClass) {
                    packetListenerConstructor = c;
                    break;
                }
            }

            // placeNewPlayer Finder
            for (Method m : playerListClass.getMethods()) {
                Class<?>[] types = m.getParameterTypes();
                if (types.length >= 2
                        && types[0] == connectionClass
                        && types[1] == serverPlayerClass) {
                    placeNewPlayerMethod = m;
                    break;
                }
            }
            if (placeNewPlayerMethod == null) throw new RuntimeException("Cannot find PlayerList.placeNewPlayer");

            initialized = true;
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.getLogger().severe("[RabiSystem/FakePlayer] NMS Init Failed: " + e.getMessage());
        }
    }

    public static Player spawnFakePlayer(Location loc, UUID uuid, String name) {
        if (!initialized) init();
        try {
            Object worldServer = getHandleWorldMethod.invoke(loc.getWorld());
            Object gameProfile = gameProfileClass.getConstructor(UUID.class, String.class).newInstance(uuid, name);
            Object clientInfo = createDefaultClientInfoMethod.invoke(null);

            // 1. Create ServerPlayer
            Object nmsPlayer = serverPlayerConstructor.newInstance(minecraftServer, worldServer, gameProfile, clientInfo);

            // 2. Setup Network (Connection)
            Object packetFlow = Enum.valueOf((Class<Enum>) packetFlowClass, "SERVERBOUND");
            Object connection = connectionConstructor.newInstance(packetFlow);

            // Inject Blind Channel
            Object blindChannel = createBlindChannel();
            channelField.set(connection, blindChannel);

            // 3. Create Cookie
            Object cookie;
            Class<?>[] cookieTypes = cookieConstructor.getParameterTypes();
            Object[] cookieArgs = new Object[cookieTypes.length];
            cookieArgs[0] = gameProfile;
            cookieArgs[1] = 0; // latency
            cookieArgs[2] = clientInfo;
            for (int i = 3; i < cookieArgs.length; i++) {
                if (cookieTypes[i] == boolean.class) cookieArgs[i] = false;
                else cookieArgs[i] = null;
            }
            cookie = cookieConstructor.newInstance(cookieArgs);

            // 4. Create PacketListener
            Class<?>[] listenerTypes = packetListenerConstructor.getParameterTypes();
            Object[] listenerArgs = new Object[listenerTypes.length];
            listenerArgs[0] = minecraftServer;
            listenerArgs[1] = connection;
            listenerArgs[2] = nmsPlayer;

            if (listenerArgs.length > 3 && listenerTypes[3].isAssignableFrom(commonListenerCookieClass)) {
                listenerArgs[3] = cookie;
            }

            for (int i = 0; i < listenerArgs.length; i++) {
                if (listenerArgs[i] != null) continue;
                Class<?> type = listenerTypes[i];
                if (type == boolean.class) listenerArgs[i] = false;
                else if (type == int.class) listenerArgs[i] = 0;
                else listenerArgs[i] = null;
            }

            Object packetListener = packetListenerConstructor.newInstance(listenerArgs);
            connectionField.set(nmsPlayer, packetListener);

            // 5. 设置坐标
            Method setPosMethod = serverPlayerClass.getMethod("setPos", double.class, double.class, double.class);
            setPosMethod.invoke(nmsPlayer, loc.getX(), loc.getY(), loc.getZ());

            // 6. 调用 PlayerList.placeNewPlayer
            Object playerList = getPlayerListMethod.invoke(minecraftServer);

            if (placeNewPlayerMethod.getParameterCount() == 3) {
                placeNewPlayerMethod.invoke(playerList, connection, nmsPlayer, cookie);
            } else {
                placeNewPlayerMethod.invoke(playerList, connection, nmsPlayer);
            }

            // 7. 返回 Bukkit 实体
            Method getBukkitEntityMethod = serverPlayerClass.getMethod("getBukkitEntity");
            return (Player) getBukkitEntityMethod.invoke(nmsPlayer);

        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.getLogger().severe("[RabiSystem/FakePlayer] Error spawning: " + e.getMessage());
            return null;
        }
    }

    private static Object createBlindChannel() {
        return Proxy.newProxyInstance(channelClass.getClassLoader(), new Class[]{channelClass}, new InvocationHandler() {
            private final Map<Object, Object> attributes = new HashMap<>();

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getName();
                if (name.equals("isOpen") || name.equals("isActive")) return true;
                if (name.equals("config")) return null;
                if (name.equals("attr")) {
                    Object key = args[0];
                    return createAttribute(key);
                }
                if (name.equals("pipeline")) {
                    return createBlindPipeline();
                }
                if (name.equals("write") || name.equals("writeAndFlush")) {
                    return createSuccessFuture();
                }
                if (name.equals("eventLoop")) {
                    return createBlindEventLoop();
                }
                Class<?> returnType = method.getReturnType();
                if (returnType == boolean.class) return false;
                if (returnType == int.class) return 0;
                if (returnType == void.class) return null;
                return null;
            }

            private Object createAttribute(Object key) {
                return Proxy.newProxyInstance(attributeClass.getClassLoader(), new Class[]{attributeClass}, (p, m, a) -> {
                    String n = m.getName();
                    if (n.equals("set")) { attributes.put(key, a[0]); return null; }
                    if (n.equals("get")) { return attributes.get(key); }
                    if (n.equals("getAndSet")) { Object old = attributes.get(key); attributes.put(key, a[0]); return old; }
                    return null;
                });
            }

            private Object createBlindPipeline() {
                return Proxy.newProxyInstance(channelPipelineClass.getClassLoader(), new Class[]{channelPipelineClass}, (p, m, a) -> {
                    if (m.getReturnType() == channelPipelineClass) return p;
                    return null;
                });
            }

            private Object createSuccessFuture() {
                return Proxy.newProxyInstance(channelFutureClass.getClassLoader(), new Class[]{channelFutureClass}, (p, m, a) -> {
                    if (m.getName().equals("isSuccess")) return true;
                    if (m.getName().equals("isDone")) return true;
                    if (m.getName().equals("cause")) return null;
                    if (m.getReturnType() == channelFutureClass) return p;
                    return null;
                });
            }

            private Object createBlindEventLoop() {
                return Proxy.newProxyInstance(eventLoopClass.getClassLoader(), new Class[]{eventLoopClass}, (p, m, a) -> {
                    if (m.getName().equals("execute") && a.length > 0 && a[0] instanceof Runnable) {
                        ((Runnable) a[0]).run();
                    }
                    if (m.getName().equals("submit") && a.length > 0 && a[0] instanceof Runnable) {
                        ((Runnable) a[0]).run();
                        return createSuccessFuture();
                    }
                    if (m.getName().equals("inEventLoop")) return true;
                    return null;
                });
            }
        });
    }
}