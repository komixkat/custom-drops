package com.komixkat.customdrops.network;

import com.komixkat.customdrops.CustomDropsMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenGuiPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenGuiPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(CustomDropsMod.MOD_ID, "open_gui"));

    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, OpenGuiPayload> CODEC =
        StreamCodec.unit(new OpenGuiPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        try {
            PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
        } catch (Throwable t) {
            CustomDropsMod.LOGGER.warn("Could not register Custom Drops open-gui payload.", t);
        }
    }
}