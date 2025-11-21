package com.modularmods.mcgltf.iris;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.modularmods.mcgltf.RenderedGltfModel;
import com.modularmods.mcgltf.RenderedGltfSceneGL30;

public class RenderedGltfSceneGL30Iris extends RenderedGltfSceneGL30 {

	@Override
	public void renderForVanilla() {
		vanillaRenderCommands.forEach(Runnable::run);
		
		RenderedGltfModel.NODE_GLOBAL_TRANSFORMATION_LOOKUP_CACHE.clear();
	}

	@Override
	public void renderForShaderMod() {
		int currentProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
		
		shaderModRenderCommands.forEach(Runnable::run);
		
		RenderedGltfModel.NODE_GLOBAL_TRANSFORMATION_LOOKUP_CACHE.clear();
	}

}
