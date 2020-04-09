package jp.jyn.jumphome;

import jp.jyn.jbukkitlib.util.PackagePrivate;
import jp.jyn.jumphome.db.Database;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Home {
    public final static String DEFAULT_NAME = "default";

    private final Map<UUID, Map<String, HomePoint>> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> user2idCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> world2idCache = new ConcurrentHashMap<>();
    private final Map<Integer, String> id2worldCache = new ConcurrentHashMap<>();

    private final Database database;

    @PackagePrivate
    Home(Database database) {
        this.database = database;
    }

    // region id
    private int user2id(UUID uuid) {
        return user2idCache.computeIfAbsent(uuid, database::user2id);
    }

    private int world2id(String name) {
        return world2idCache.computeIfAbsent(name, n -> {
            int i = database.world2Id(n);
            id2worldCache.put(i, n);
            return i;
        });
    }

    // ↓IDに紐づく世界がない時にnull(=同じ処理で何度もデータベースが呼ばれる)
    // ↓自動採番でオブジェクト<->idを紐づけてるだけなので、nullになる可能性は非常に低い(手でDBを壊すとかしない限りあり得ない)
    private String id2world(int id) {
        return id2worldCache.computeIfAbsent(id, i -> {
            String world = database.id2world(i);
            if (world != null) {
                world2idCache.put(world, i);
            }
            return world;
        });
    }
    // endregion

    // region for internal use
    // 入力補完をやろうと思うとこの処理が必要になる
    @PackagePrivate
    void loadCache(UUID uuid) {
        Map<String, HomePoint> userCache = cache.computeIfAbsent(uuid, u -> new ConcurrentHashMap<>());
        for (Database.RawLocation location : database.list(user2id(uuid))) {
            HomePoint home = toHomePoint(location);
            userCache.put(home.name, home);
        }
    }

    @PackagePrivate
    void unloadCache(UUID uuid) {
        cache.remove(uuid);
    }

    @PackagePrivate
    Set<String> cachedName(UUID uuid) {
        return cache.getOrDefault(uuid, Collections.emptyMap()).keySet();
    }
    // endregion

    public boolean set(UUID uuid, String name, Location location) {
        if (uuid == null || name == null || location == null) {
            throw new NullPointerException();
        }

        if (database.set(
            user2id(uuid),
            name,
            world2id(Objects.requireNonNull(location.getWorld(), "getWorld() is null").getName()),
            location.getX(),
            location.getY(),
            location.getZ(),
            location.getYaw()
        )) {
            // キャッシュ更新
            HomePoint home = new HomePoint(
                name,
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw()
            );
            cache.computeIfAbsent(uuid, u -> new ConcurrentHashMap<>()).put(name, home);
            return true;
        }
        return false;
    }

    public Optional<HomePoint> get(UUID uuid, String name) {
        if (uuid == null || name == null) {
            throw new NullPointerException();
        }

        // キャッシュ検索
        HomePoint home = cache.getOrDefault(uuid, Collections.emptyMap()).get(name);
        if (home != null) {
            return Optional.of(home);
        }

        // 問い合わせ
        Database.RawLocation location = database.get(user2id(uuid), name);
        if (location != null) {
            // キャッシュ更新
            home = toHomePoint(location);
            cache.computeIfAbsent(uuid, u -> new ConcurrentHashMap<>()).put(name, home);
        }

        return Optional.ofNullable(home);
    }

    public boolean exists(UUID uuid, String name) {
        return get(uuid, name).isPresent();
    }

    public boolean delete(UUID uuid, String name) {
        if (uuid == null || name == null) {
            throw new NullPointerException();
        }

        // 削除
        cache.getOrDefault(uuid, Collections.emptyMap()).remove(name);
        return database.delete(user2id(uuid), name);
    }

    public List<HomePoint> list(UUID uuid) {
        List<Database.RawLocation> locations = database.list(user2id(Objects.requireNonNull(uuid)));

        // 0なら何もしない
        if (locations.size() == 0) {
            return Collections.emptyList();
        }

        // 1ならキャッシュしてsingletonListで返却
        Map<String, HomePoint> userCache = cache.computeIfAbsent(uuid, u -> new ConcurrentHashMap<>());
        if (locations.size() == 1) {
            HomePoint home = toHomePoint(locations.get(0));
            userCache.put(home.name, home);
            return Collections.singletonList(home);
        }

        // キャッシュしながら入れていく
        List<HomePoint> result = new ArrayList<>(locations.size());
        for (Database.RawLocation location : locations) {
            HomePoint home = toHomePoint(location);
            userCache.put(home.name, home);
            result.add(home);
        }

        return result;
    }

    public boolean set(OfflinePlayer player, String name, Location location) {
        return set(player.getUniqueId(), name, location);
    }

    public Optional<HomePoint> get(OfflinePlayer player, String name) {
        return get(player.getUniqueId(), name);
    }

    public boolean exists(OfflinePlayer player, String name) {
        return exists(player.getUniqueId(), name);
    }

    public boolean delete(OfflinePlayer player, String name) {
        return delete(player.getUniqueId(), name);
    }

    public List<HomePoint> list(OfflinePlayer player) {
        return list(player.getUniqueId());
    }

    private HomePoint toHomePoint(Database.RawLocation location) {
        return new HomePoint(
            location.name,
            Objects.requireNonNull(id2world(location.world), "Database broken (non-existent world ID)"),
            location.x,
            location.y,
            location.z,
            location.yaw
        );
    }

    public static class HomePoint {
        public final String name;
        public final String world;
        public final double x;
        public final double y;
        public final double z;
        public final float yaw;

        private HomePoint(String name, String world, double x, double y, double z, float yaw) {
            this.name = Objects.requireNonNull(name);
            this.world = Objects.requireNonNull(world);
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
        }

        public Optional<World> getBukkitWorld() {
            return Optional.ofNullable(Bukkit.getWorld(world));
        }

        public boolean isWorldLoaded() {
            return Bukkit.getWorld(name) != null;
        }

        public Location toLocation() {
            return new Location(getBukkitWorld().orElse(null), x, y, z, yaw, 0);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HomePoint homePoint = (HomePoint) o;
            return Double.compare(homePoint.x, x) == 0 &&
                Double.compare(homePoint.y, y) == 0 &&
                Double.compare(homePoint.z, z) == 0 &&
                Float.compare(homePoint.yaw, yaw) == 0 &&
                name.equals(homePoint.name) &&
                world.equals(homePoint.world);
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            result = name.hashCode();
            result = 31 * result + world.hashCode();
            temp = Double.doubleToLongBits(x);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(y);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(z);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            result = 31 * result + (yaw != +0.0f ? Float.floatToIntBits(yaw) : 0);
            return result;
        }

        @Override
        public String toString() {
            return "HomePoint{" +
                "name='" + name + '\'' +
                ", world='" + world + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", yaw=" + yaw +
                '}';
        }
    }
}
