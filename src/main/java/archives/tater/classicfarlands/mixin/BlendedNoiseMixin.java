package archives.tater.classicfarlands.mixin;

import archives.tater.classicfarlands.ClassicFarlands;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;

/**
 * Original approach used @ModifyExpressionValue to intercept the blockX()/blockZ()
 * INVOKE call sites *inside* compute()'s body. That breaks when another mod (e.g.
 * C2ME's natives-math module) fully replaces compute()'s body via @Overwrite, since
 * the original call sites this mixin was looking for no longer exist post-overwrite.
 *
 * This version wraps the incoming FunctionContext parameter itself at method HEAD,
 * before any body (vanilla or overwritten) runs. Since @Overwrite implementations
 * generally still read coordinates off the same context object passed as the
 * parameter, wrapping it here should apply the Far Lands distortion regardless of
 * which implementation of compute() ultimately executes.
 *
 * VERIFIED against your actual installed jars (not just GitHub source):
 * decompiled both classicfarlands-1.2.3+mc26.1.jar and the natives-math
 * submodule bundled inside c2me-fabric-mc26.2-0.4.2-alpha.0.52.jar. C2ME's
 * @Overwrite body really does call FunctionContext.blockX()/blockY()/blockZ()
 * directly (confirmed via javap on the compiled bytecode, constant pool
 * entries #70/#73/#76), matching this mixin's target exactly. Across every
 * usage in both mods' bytecode, FunctionContext is only ever called for
 * those three methods -- no getBlender() or similar shows up anywhere, so
 * it's intentionally left out below rather than guessed at.
 */
@Mixin(BlendedNoise.class)
public class BlendedNoiseMixin {

    @ModifyVariable(method = "compute", at = @At("HEAD"), argsOnly = true)
    private DensityFunction.FunctionContext wrapContext(DensityFunction.FunctionContext original) {
        return new DensityFunction.FunctionContext() {
            @Override
            public int blockX() {
                return ClassicFarlands.adjustCoordinate(original.blockX());
            }

            @Override
            public int blockY() {
                return original.blockY();
            }

            @Override
            public int blockZ() {
                return ClassicFarlands.adjustCoordinate(original.blockZ());
            }
        };
    }
}
