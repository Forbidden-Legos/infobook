package de.olivermakesco.infobook.mixin;

import de.olivermakesco.infobook.InfobookModKt;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class Mixin_ServerPlayerEntity {
	@Inject(
			method = "onSpawn()V",
			at = @At("RETURN")
	)
	private void infobook$onPlayerJoin(CallbackInfo ci) {
		InfobookModKt.onPlayerJoin((ServerPlayerEntity) (Object) this);
	}
}
