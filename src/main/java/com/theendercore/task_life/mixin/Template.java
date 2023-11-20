package com.theendercore.task_life.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.theendercore.task_life.TaskLife.LOG;

@Mixin(MinecraftClient.class)
public class Template {

    @Inject(at = @At("HEAD"), method = "run")
    private void run(CallbackInfo info) {
        LOG.info("Hello from Mixin");
    }
}
