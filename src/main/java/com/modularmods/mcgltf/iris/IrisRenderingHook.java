package com.modularmods.mcgltf.iris;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.irisshaders.batchedentityrendering.impl.WrappableRenderType;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import com.modularmods.mcgltf.RenderedGltfModel;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;

public class IrisRenderingHook {

	private static final Map<WorldRenderingPhase, Map<RenderStateShard, List<Runnable>>> phaseRenderStateShardToCommands = new EnumMap<WorldRenderingPhase, Map<RenderStateShard, List<Runnable>>>(WorldRenderingPhase.class);
	private static RenderStateShard currentRenderStateShard;
	
	public static void submitCommandForIrisRenderingByPhaseName(String phase, RenderType renderType, Runnable command) {
		submitCommandForIrisRendering(WorldRenderingPhase.valueOf(phase), renderType, command);
	}
	
	public static void submitCommandForIrisRendering(WorldRenderingPhase phase, RenderType renderType, Runnable command) {
		Map<RenderStateShard, List<Runnable>> renderStateShardToCommands = phaseRenderStateShardToCommands.get(phase);
		if(renderStateShardToCommands == null) {
			renderStateShardToCommands = new LinkedHashMap<RenderStateShard, List<Runnable>>();
			phaseRenderStateShardToCommands.put(phase, renderStateShardToCommands);
			List<Runnable> commands = new LinkedList<Runnable>();
			renderStateShardToCommands.put(renderType, commands);
			commands.add(command);
		}
		else {
			List<Runnable> commands = renderStateShardToCommands.get(renderType);
			if(commands == null) {
				commands = new LinkedList<Runnable>();
				renderStateShardToCommands.put(renderType, commands);
			}
			commands.add(command);
		}
	}
	
	public static void irisHookAfterSetupRenderState(RenderStateShard renderStateShard) {
		currentRenderStateShard = ((renderStateShard instanceof WrappableRenderType) ? ((WrappableRenderType)renderStateShard).unwrap() : renderStateShard);
	}
	
	public static void irisHookBypassBufferUploaderVextexCountCheck(BufferBuilder.RenderedBuffer renderedBuffer) {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
		if(pipeline != null) {
			WorldRenderingPhase phase = pipeline.getPhase();
			Map<RenderStateShard, List<Runnable>> renderStateShardToCommands = phaseRenderStateShardToCommands.get(phase);
			if(renderStateShardToCommands != null) {
				List<Runnable> commands = renderStateShardToCommands.remove(currentRenderStateShard);
				if(commands != null) {
					ShaderInstance shaderInstance = RenderedGltfModel.CURRENT_SHADER_INSTANCE = RenderSystem.getShader();
					for (int i = 0; i < 12; ++i) {
						int j = RenderSystem.getShaderTexture(i);
						shaderInstance.setSampler("Sampler" + i, j);
					}
					if (shaderInstance.MODEL_VIEW_MATRIX != null) {
						shaderInstance.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
					}
					if (shaderInstance.PROJECTION_MATRIX != null) {
						shaderInstance.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
					}
					if (shaderInstance.INVERSE_VIEW_ROTATION_MATRIX != null) {
						shaderInstance.INVERSE_VIEW_ROTATION_MATRIX.set(RenderSystem.getInverseViewRotationMatrix());
					}
					if (shaderInstance.COLOR_MODULATOR != null) {
						shaderInstance.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
					}
					if (shaderInstance.FOG_START != null) {
						shaderInstance.FOG_START.set(RenderSystem.getShaderFogStart());
					}
					if (shaderInstance.FOG_END != null) {
						shaderInstance.FOG_END.set(RenderSystem.getShaderFogEnd());
					}
					if (shaderInstance.FOG_COLOR != null) {
						shaderInstance.FOG_COLOR.set(RenderSystem.getShaderFogColor());
					}
					if (shaderInstance.FOG_SHAPE != null) {
						shaderInstance.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
					}
					if (shaderInstance.TEXTURE_MATRIX != null) {
						shaderInstance.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
					}
					if (shaderInstance.GAME_TIME != null) {
						shaderInstance.GAME_TIME.set(RenderSystem.getShaderGameTime());
					}
					if (shaderInstance.SCREEN_SIZE != null) {
						Window window = Minecraft.getInstance().getWindow();
						shaderInstance.SCREEN_SIZE.set((float)window.getWidth(), (float)window.getHeight());
					}
					if (shaderInstance.LINE_WIDTH != null) {
						VertexFormat.Mode mode = renderedBuffer.drawState().mode();
						if(mode == VertexFormat.Mode.LINES || mode == VertexFormat.Mode.LINE_STRIP) {
							shaderInstance.LINE_WIDTH.set(RenderSystem.getShaderLineWidth());
						}
					}
					RenderSystem.setupShaderLights(shaderInstance);
					shaderInstance.apply();
					
					setupAndRender(phase, shaderInstance, commands);
					
					shaderInstance.clear();
				}
			}
		}
	}
	
	public static void irisHookAfterVertexBufferDraw() {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
		if(pipeline != null) {
			WorldRenderingPhase phase = pipeline.getPhase();
			Map<RenderStateShard, List<Runnable>> renderStateShardToCommands = phaseRenderStateShardToCommands.get(phase);
			if(renderStateShardToCommands != null) {
				List<Runnable> commands = renderStateShardToCommands.remove(currentRenderStateShard);
				if(commands != null) {
					setupAndRender(phase, RenderedGltfModel.CURRENT_SHADER_INSTANCE = RenderSystem.getShader(), commands);
				}
			}
		}
	}
	
	private static void setupAndRender(WorldRenderingPhase phase, ShaderInstance shaderInstance, List<Runnable> commands) {
		int currentVAO = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
		int currentArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
		int currentElementArrayBuffer = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
		
		boolean currentCullFace = GL11.glGetBoolean(GL11.GL_CULL_FACE);
		
		if(phase != WorldRenderingPhase.NONE) {
			int currentProgram = shaderInstance.getId();
			RenderedGltfModel.MODEL_VIEW_MATRIX = GL20.glGetUniformLocation(currentProgram, "iris_ModelViewMat");
			RenderedGltfModel.NORMAL_MATRIX = GL20.glGetUniformLocation(currentProgram, "iris_NormalMat");
			int normals = GL20.glGetUniformLocation(currentProgram, "normals");
			int specular = GL20.glGetUniformLocation(currentProgram, "specular");
			
			int currentTextureColor;
			
			if(normals != -1) {
				RenderedGltfModel.NORMAL_MAP_INDEX = GL13.GL_TEXTURE0 + GL20.glGetUniformi(currentProgram, normals);
				if(specular != -1) {
					RenderedGltfModel.SPECULAR_MAP_INDEX = GL13.GL_TEXTURE0 + GL20.glGetUniformi(currentProgram, specular);
					
					GL13.glActiveTexture(RenderedGltfModel.NORMAL_MAP_INDEX);
					int currentTextureNormal = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
					GL13.glActiveTexture(RenderedGltfModel.SPECULAR_MAP_INDEX);
					int currentTextureSpecular = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
					GL13.glActiveTexture(GL13.GL_TEXTURE0);
					currentTextureColor = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
					
					commands.forEach(Runnable::run);
					
					GL13.glActiveTexture(RenderedGltfModel.NORMAL_MAP_INDEX);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTextureNormal);
					GL13.glActiveTexture(RenderedGltfModel.SPECULAR_MAP_INDEX);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTextureSpecular);
				}
				else {
					RenderedGltfModel.SPECULAR_MAP_INDEX = -1;
					
					GL13.glActiveTexture(RenderedGltfModel.NORMAL_MAP_INDEX);
					int currentTextureNormal = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
					GL13.glActiveTexture(GL13.GL_TEXTURE0);
					currentTextureColor = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
					
					commands.forEach(Runnable::run);
					
					GL13.glActiveTexture(RenderedGltfModel.NORMAL_MAP_INDEX);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTextureNormal);
				}
			}
			else {
				RenderedGltfModel.NORMAL_MAP_INDEX = -1;
				if(specular != -1) {
					RenderedGltfModel.SPECULAR_MAP_INDEX = GL13.GL_TEXTURE0 + GL20.glGetUniformi(currentProgram, specular);
					
					GL13.glActiveTexture(RenderedGltfModel.SPECULAR_MAP_INDEX);
					int currentTextureSpecular = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
					GL13.glActiveTexture(GL13.GL_TEXTURE0);
					currentTextureColor = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
					
					commands.forEach(Runnable::run);
					
					GL13.glActiveTexture(RenderedGltfModel.SPECULAR_MAP_INDEX);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTextureSpecular);
				}
				else {
					RenderedGltfModel.SPECULAR_MAP_INDEX = -1;
					
					GL13.glActiveTexture(GL13.GL_TEXTURE0);
					currentTextureColor = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
					
					commands.forEach(Runnable::run);
				}
			}
			
			GL13.glActiveTexture(GL13.GL_TEXTURE0);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTextureColor);
		}
		else {
			RenderedGltfModel.LIGHT0_DIRECTION = new Vector3f(shaderInstance.LIGHT0_DIRECTION.getFloatBuffer());
			RenderedGltfModel.LIGHT1_DIRECTION = new Vector3f(shaderInstance.LIGHT1_DIRECTION.getFloatBuffer());
			
			GL13.glActiveTexture(GL13.GL_TEXTURE0);
			int currentTextureColor = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
			
			commands.forEach(Runnable::run);
			
			GL13.glActiveTexture(GL13.GL_TEXTURE0);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTextureColor);
		}
		
		if(currentCullFace) GL11.glEnable(GL11.GL_CULL_FACE);
		else GL11.glDisable(GL11.GL_CULL_FACE);
		
		GL30.glBindVertexArray(currentVAO);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, currentArrayBuffer);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, currentElementArrayBuffer);
	}
}
