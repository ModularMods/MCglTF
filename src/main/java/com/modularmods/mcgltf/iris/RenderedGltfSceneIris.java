package com.modularmods.mcgltf.iris;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL43;

import com.modularmods.mcgltf.MCglTF;
import com.modularmods.mcgltf.RenderedGltfModel;
import com.modularmods.mcgltf.RenderedGltfScene;

public class RenderedGltfSceneIris extends RenderedGltfScene {

	@Override
	public void renderForVanilla() {
		if(!skinningCommands.isEmpty()) {
			GL20.glUseProgram(MCglTF.getInstance().getGlProgramSkinnig());
			GL11.glEnable(GL30.GL_RASTERIZER_DISCARD);
			skinningCommands.forEach(Runnable::run);
			GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
			GL40.glBindTransformFeedback(GL40.GL_TRANSFORM_FEEDBACK, 0);
			GL11.glDisable(GL30.GL_RASTERIZER_DISCARD);
			GL20.glUseProgram(RenderedGltfModel.CURRENT_SHADER_INSTANCE.getId());
		}
		
		vanillaRenderCommands.forEach(Runnable::run);
		
		RenderedGltfModel.NODE_GLOBAL_TRANSFORMATION_LOOKUP_CACHE.clear();
	}

	@Override
	public void renderForShaderMod() {
		int currentProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
		
		if(!skinningCommands.isEmpty()) {
			GL20.glUseProgram(MCglTF.getInstance().getGlProgramSkinnig());
			GL11.glEnable(GL30.GL_RASTERIZER_DISCARD);
			skinningCommands.forEach(Runnable::run);
			GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
			GL40.glBindTransformFeedback(GL40.GL_TRANSFORM_FEEDBACK, 0);
			GL11.glDisable(GL30.GL_RASTERIZER_DISCARD);
			GL20.glUseProgram(currentProgram);
		}

        shaderModRenderCommands.forEach(Runnable::run);
		
		RenderedGltfModel.NODE_GLOBAL_TRANSFORMATION_LOOKUP_CACHE.clear();
	}

}
