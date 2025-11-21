package com.modularmods.mcgltf.iris;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Triple;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import com.google.gson.Gson;
import com.modularmods.mcgltf.MCglTF;
import com.modularmods.mcgltf.RenderedGltfModel;

import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SceneModel;
import de.javagl.jgltf.model.TextureModel;
import de.javagl.jgltf.model.v2.MaterialModelV2;

public class RenderedGltfModelIris extends RenderedGltfModel {

	public RenderedGltfModelIris(List<Runnable> gltfRenderData, GltfModel gltfModel) {
		super(gltfRenderData, gltfModel);
	}

	@Override
	protected void processSceneModels(List<Runnable> gltfRenderData, List<SceneModel> sceneModels) {
		for(SceneModel sceneModel : sceneModels) {
			RenderedGltfSceneIris renderedGltfScene = new RenderedGltfSceneIris();
			renderedGltfScenes.add(renderedGltfScene);
			
			for(NodeModel nodeModel : sceneModel.getNodeModels()) {
				Triple<List<Runnable>, List<Runnable>, List<Runnable>> commands = rootNodeModelToCommands.get(nodeModel);
				List<Runnable> rootSkinningCommands;
				List<Runnable> vanillaRootRenderCommands;
				List<Runnable> shaderModRootRenderCommands;
				if(commands == null) {
					rootSkinningCommands = new ArrayList<Runnable>();
					vanillaRootRenderCommands = new ArrayList<Runnable>();
					shaderModRootRenderCommands = new ArrayList<Runnable>();
					processNodeModel(gltfRenderData, nodeModel, rootSkinningCommands, vanillaRootRenderCommands, shaderModRootRenderCommands);
					rootNodeModelToCommands.put(nodeModel, Triple.of(rootSkinningCommands, vanillaRootRenderCommands, shaderModRootRenderCommands));
				}
				else {
					rootSkinningCommands = commands.getLeft();
					vanillaRootRenderCommands = commands.getMiddle();
					shaderModRootRenderCommands = commands.getRight();
				}
				renderedGltfScene.skinningCommands.addAll(rootSkinningCommands);
				renderedGltfScene.vanillaRenderCommands.addAll(vanillaRootRenderCommands);
				renderedGltfScene.shaderModRenderCommands.addAll(shaderModRootRenderCommands);
			}
		}
	}

	@Override
	protected void applyTransformShaderMod(NodeModel nodeModel) {
		float[] transform = findGlobalTransform(nodeModel);
		Matrix4f pose = new Matrix4f();
		pose.setTransposed(transform);
		
		if(NORMAL_MATRIX != -1) {
			Matrix3f normal = new Matrix3f(pose);
			normal.transpose();
			normal.mulLocal(CURRENT_NORMAL);
			
			normal.get(BUF_FLOAT_9);
			GL20.glUniformMatrix3fv(NORMAL_MATRIX, false, BUF_FLOAT_9);
		}
		
		pose.transpose();
		pose.mulLocal(CURRENT_POSE);
		
		pose.get(BUF_FLOAT_16);
		GL20.glUniformMatrix4fv(MODEL_VIEW_MATRIX, false, BUF_FLOAT_16);
	}

	@Override
	public Material obtainMaterial(List<Runnable> gltfRenderData, MaterialModel materialModel) {
		Material material = materialModelToRenderedMaterial.get(materialModel);
		if(material == null) {
			Object extras = materialModel.getExtras();
			if(extras != null) {
				Gson gson = new Gson();
				material = gson.fromJson(gson.toJsonTree(extras), MaterialIris.class);
			}
			else material = new MaterialIris();
			material.initMaterialCommand(gltfRenderData, this, materialModel);
			materialModelToRenderedMaterial.put(materialModel, material);
		}
		return material;
	}
	
	public static class MaterialIris extends Material {

		@Override
		public void initMaterialCommand(List<Runnable> gltfRenderData, RenderedGltfModel renderedModel, MaterialModel materialModel) {
			int colorMap;
			int normalMap;
			int specularMap;
			List<TextureModel> textureModels = renderedModel.gltfModel.getTextureModels();
			if(materialModel instanceof MaterialModelV2) {
				MaterialModelV2 materialModelV2 = (MaterialModelV2) materialModel;
				
				if(baseColorTexture == null) {
					TextureModel textureModel = materialModelV2.getBaseColorTexture();
					if(textureModel != null) {
						colorMap = renderedModel.obtainGlTexture(gltfRenderData, textureModel);
						baseColorTexture = new TextureInfo();
						baseColorTexture.index = textureModels.indexOf(textureModel);
					}
					else colorMap = MCglTF.getInstance().getDefaultColorMap();
				}
				else colorMap = renderedModel.obtainGlTexture(gltfRenderData, textureModels.get(baseColorTexture.index));
				
				if(normalTexture == null) {
					TextureModel textureModel = materialModelV2.getNormalTexture();
					if(textureModel != null) {
						normalMap = renderedModel.obtainGlTexture(gltfRenderData, textureModel);
						normalTexture = new TextureInfo();
						normalTexture.index = textureModels.indexOf(textureModel);
					}
					else normalMap = MCglTF.getInstance().getDefaultNormalMap();
				}
				else normalMap = renderedModel.obtainGlTexture(gltfRenderData, textureModels.get(normalTexture.index));
				
				if(specularTexture == null) {
					TextureModel textureModel = materialModelV2.getMetallicRoughnessTexture();
					if(textureModel != null) {
						specularMap = renderedModel.obtainGlTexture(gltfRenderData, textureModel);
						specularTexture = new TextureInfo();
						specularTexture.index = textureModels.indexOf(textureModel);
					}
					else specularMap = MCglTF.getInstance().getDefaultSpecularMap();
				}
				else specularMap = renderedModel.obtainGlTexture(gltfRenderData, textureModels.get(specularTexture.index));
				
				if(baseColorFactor == null) baseColorFactor = materialModelV2.getBaseColorFactor();
				
				if(doubleSided == null) doubleSided = materialModelV2.isDoubleSided();
			}
			else {
				colorMap = baseColorTexture == null ? MCglTF.getInstance().getDefaultColorMap() : renderedModel.obtainGlTexture(gltfRenderData, textureModels.get(baseColorTexture.index));
				normalMap = normalTexture == null ? MCglTF.getInstance().getDefaultNormalMap() : renderedModel.obtainGlTexture(gltfRenderData, textureModels.get(normalTexture.index));
				specularMap = specularTexture == null ? MCglTF.getInstance().getDefaultSpecularMap() : renderedModel.obtainGlTexture(gltfRenderData, textureModels.get(specularTexture.index));
				if(baseColorFactor == null) baseColorFactor = new float[]{1.0F, 1.0F, 1.0F, 1.0F};
				if(doubleSided == null) doubleSided = false;
			}
			
			if(doubleSided) {
				vanillaMaterialCommand = () -> {
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorMap);
					GL20.glVertexAttrib4f(vaColor, baseColorFactor[0], baseColorFactor[1], baseColorFactor[2], baseColorFactor[3]);

					// Enhanced transparency handling for vanilla rendering
					if(baseColorFactor[3] < 1.0f) {
						GL11.glEnable(GL11.GL_BLEND);
						GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
					} else {
						GL11.glDisable(GL11.GL_BLEND);
					}
					
					GL11.glDisable(GL11.GL_CULL_FACE);
				};
				shaderModMaterialCommand = () -> {
					GL13.glActiveTexture(COLOR_MAP_INDEX);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorMap);
					
					if(NORMAL_MAP_INDEX != -1) {
						GL13.glActiveTexture(NORMAL_MAP_INDEX);
						GL11.glBindTexture(GL11.GL_TEXTURE_2D, normalMap);
					}
					if(SPECULAR_MAP_INDEX != -1) {
						GL13.glActiveTexture(SPECULAR_MAP_INDEX);
						GL11.glBindTexture(GL11.GL_TEXTURE_2D, specularMap);
					}

					// Enhanced color attribute with proper alpha handling
					GL20.glVertexAttrib4f(vaColor, baseColorFactor[0], baseColorFactor[1],
										  baseColorFactor[2], baseColorFactor[3]);

					// Enhanced transparency handling for Iris shader rendering
					if(baseColorFactor[3] < 1.0f) {
						GL11.glEnable(GL11.GL_BLEND);
						GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
					} else {
						GL11.glDisable(GL11.GL_BLEND);
					}
					
					// Add entityColor uniform support for shader packs like SEUS PTGI
					int currentProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
					int entityColorLocation = GL20.glGetUniformLocation(currentProgram, "entityColor");
					if (entityColorLocation != -1) {
						GL20.glUniform4f(entityColorLocation, 1.0f, 1.0f, 1.0f, 0.0f);
					}

					GL11.glDisable(GL11.GL_CULL_FACE);
				};
			}
			else {
				vanillaMaterialCommand = () -> {
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorMap);
					GL20.glVertexAttrib4f(vaColor, baseColorFactor[0], baseColorFactor[1], baseColorFactor[2], baseColorFactor[3]);
					
					// Enhanced transparency handling for vanilla rendering
					if(baseColorFactor[3] < 1.0f) {
						GL11.glEnable(GL11.GL_BLEND);
						GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
					} else {
						GL11.glDisable(GL11.GL_BLEND);
					}
					
					GL11.glEnable(GL11.GL_CULL_FACE);
				};
				shaderModMaterialCommand = () -> {
					GL13.glActiveTexture(COLOR_MAP_INDEX);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorMap);
					
					if(NORMAL_MAP_INDEX != -1) {
						GL13.glActiveTexture(NORMAL_MAP_INDEX);
						GL11.glBindTexture(GL11.GL_TEXTURE_2D, normalMap);
					}
					if(SPECULAR_MAP_INDEX != -1) {
						GL13.glActiveTexture(SPECULAR_MAP_INDEX);
						GL11.glBindTexture(GL11.GL_TEXTURE_2D, specularMap);
					}

					// Enhanced color attribute with proper alpha handling
					GL20.glVertexAttrib4f(vaColor, baseColorFactor[0], baseColorFactor[1],
										  baseColorFactor[2], baseColorFactor[3]);

					// Enhanced transparency handling for Iris shader rendering
					if(baseColorFactor[3] < 1.0f) {
						GL11.glEnable(GL11.GL_BLEND);
						GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
					} else {
						GL11.glDisable(GL11.GL_BLEND);
					}
					
					// Add entityColor uniform support for shader packs like SEUS PTGI
					int currentProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
					int entityColorLocation = GL20.glGetUniformLocation(currentProgram, "entityColor");
					if (entityColorLocation != -1) {
						GL20.glUniform4f(entityColorLocation, 1.0f, 1.0f, 1.0f, 0.0f);
					}

					GL11.glEnable(GL11.GL_CULL_FACE);
				};
			}
		}
		
	}

}
