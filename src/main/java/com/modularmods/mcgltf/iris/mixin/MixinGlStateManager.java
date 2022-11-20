package com.modularmods.mcgltf.iris.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.modularmods.mcgltf.iris.IrisRenderingHook;
import com.mojang.blaze3d.platform.GlStateManager;

@Mixin(GlStateManager.class)
public class MixinGlStateManager {

	@Inject(method = "_drawArrays(III)V", at = @At("TAIL"))
	private static void afterDrawArrays(int mode, int first, int count, CallbackInfo ci) {
		IrisRenderingHook.irisHookAfterDrawArrays();
	}
}
