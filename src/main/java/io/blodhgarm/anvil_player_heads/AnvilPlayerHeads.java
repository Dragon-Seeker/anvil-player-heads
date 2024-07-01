package io.blodhgarm.anvil_player_heads;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.ProfileLookupCallback;
import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.StringHelper;
import net.minecraft.util.Uuids;
import net.minecraft.util.collection.DefaultedList;

import java.util.Collection;
import java.util.Optional;

public class AnvilPlayerHeads implements ModInitializer {

    public static final String MODID = "anvil_player_heads";

    public void onInitialize() {}

    public static boolean handleHeadRename(PlayerEntity player, DefaultedList<Slot> slots, String playerName) {
        var left = slots.get(0).getStack();

        if (left.getItem() != Items.PLAYER_HEAD || !slots.get(1).getStack().isEmpty() || !(player instanceof ServerPlayerEntity serverPlayer)) return false;

        var output = left.copy();
        var nbt = output.getOrCreateNbt();

        if (getGameProfile(serverPlayer.server, playerName).isPresent()) {
            nbt.putString("SkullOwner", playerName);

            output.setNbt(nbt);

            slots.get(2).setStack(output);

            return true;
        } else {
            nbt.remove("SkullOwner");

            output.setNbt(nbt);

            slots.get(2).setStack(output);

            return false;
        }
    }

    private static Optional<GameProfile> getGameProfile(MinecraftServer server, String name) {
        if (StringHelper.isEmpty(name) || name.length() > 16) return Optional.empty();

        return server.getUserCache()
                .findByName(name)
                .map(GameProfile::getId)
                .or(() -> {
                    final var list = Lists.<GameProfile>newArrayList();

                    var profileLookupCallback = new ProfileLookupCallback() {
                        @Override
                        public void onProfileLookupSucceeded(GameProfile profile) {
                            server.getUserCache().add(profile);
                            list.add(profile);
                        }

                        @Override
                        public void onProfileLookupFailed(String profileName, Exception exception) {}
                    };

                    lookupProfile(server, Lists.newArrayList(name), profileLookupCallback);

                    if(!list.isEmpty()) {
                        var id = list.get(0).getId();

                        if(id != null) return Optional.of(id);
                    }

                    return Optional.empty();
                }).map(uuid -> new GameProfile(uuid, name));
    }

    private static void lookupProfile(MinecraftServer server, Collection<String> players, ProfileLookupCallback callback) {
        var names = players.stream().filter(playerName -> !StringHelper.isEmpty(playerName)).toArray(String[]::new);

        if (server.isOnlineMode()) {
            server.getGameProfileRepo().findProfilesByNames(names, callback);
        }  else {
            for (var name : names) {
                callback.onProfileLookupSucceeded(new GameProfile(Uuids.getOfflinePlayerUuid(name), name));
            }
        }
    }
}
