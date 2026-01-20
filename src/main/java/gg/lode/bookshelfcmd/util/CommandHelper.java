package gg.lode.bookshelfcmd.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CommandHelper {

    public static OfflinePlayer getOfflinePlayerOrPlayer(UUID uniqueId) {
        return uniqueId == null ? null : Bukkit.getPlayer(uniqueId) != null ? Bukkit.getPlayer(uniqueId) : Bukkit.getOfflinePlayer(uniqueId);
    }

    @Nullable
    public static OfflinePlayer convertPlayerProfileToOfflinePlayer(List<PlayerProfile> profiles) {
        PlayerProfile playerProfile = profiles.size() > 0 ? profiles.get(0) : null;
        if (playerProfile == null || playerProfile.getUniqueId() == null) return null;

        return getOfflinePlayerOrPlayer(playerProfile.getUniqueId());
    }

    @Nullable
    public static List<OfflinePlayer> convertPlayerProfileToOfflinePlayers(List<PlayerProfile> profiles) {
        return profiles.stream().map(p -> getOfflinePlayerOrPlayer(p.getUniqueId())).filter(Objects::nonNull).toList();
    }

}
