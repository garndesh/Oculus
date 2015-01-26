package garndesh.openglrenderer;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_SHORT;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.BufferUtils;

public class CubeRenderer {

	private int vaoID;
	private int vboID;
	private int vboTexID;
	private int eboID;
	private Texture texture;

	public CubeRenderer() {
		// Vertices for our cube
		FloatBuffer vertices = BufferUtils.createFloatBuffer(24);
		vertices.put(new float[] { 
				-0.5f, +0.5f, +0.5f, // ID: 0
				+0.5f, +0.5f, +0.5f, // ID: 1
				-0.5f, -0.5f, +0.5f, // ID: 2
				+0.5f, -0.5f, +0.5f, // ID: 3
				+0.5f, +0.5f, -0.5f, // ID: 4
				+0.5f, -0.5f, -0.5f, // ID: 5
				-0.5f, +0.5f, -0.5f, // ID: 6
				-0.5f, -0.5f, -0.5f // ID: 7
		});
		vertices.rewind();

		/*// Colors for our cube
		FloatBuffer colors = BufferUtils.createFloatBuffer(32);
		colors.put(new float[] { 1, 0, 0, 0, // 1
				0, 1, 0, 0, // 2
				0, 0, 1, 0, // 3
				1, 1, 1, 0, // 4
				1, 0, 0, 0, // 5
				0, 1, 0, 0, // 6
				0, 0, 1, 0, // 7
				1, 1, 1, 0 }); // 8
		colors.rewind();*/
		
		FloatBuffer textureCoords = BufferUtils.createFloatBuffer(12*6);
		textureCoords.put(new float[] {
				0.667F, 0.667F, 1F, 0.667F, 0.667F, 1F, 1F, 1F, 
				

				0.3333F, 0.6667F, 0.3333F, 1.0000F,
				
				
				0.3333F, 0.3333F, 
				0.6667F, 0.6667F, 0.3333F, 0.6667F, 0.3333F, 0.3333F, 
				
				0.667F, 0.667F, 1F, 0.667F, 0.667F, 1F,
				1F, 1F, 0.667F, 1F, 1F, 0.667F,
				0.667F, 1F, 1F, 0.667F,
				
				0.3333F, 0, 0.3333F, 0.3333F, 0, 0, 
				0.3333F, 0.3333F, 0, 0.3333F, 0, 0, 
				0.6667F, 0, 0.6667F, 0.3333F, 0.3333F, 0,
				0.6667F, 0.3333F, 0.3333F, 0.3333F, 0.3333F, 0, 
				1 ,0, 1, 0.3333F, 0.6667F, 0, 
				1, 0.3333F, 0.6667F, 0.3333F, 0.6667F, 0
		});
		textureCoords.rewind();

		// Elements for our cube
		ShortBuffer elements = BufferUtils.createShortBuffer(36);
		elements.put(new short[] { 0, 1, 2, 3, 2, 1, // Front face
				1, 4, 3, 3, 5, 4, // Right face
				4, 6, 5, 5, 7, 6, // Back face
				6, 0, 7, 7, 2, 0, // Left face
				6, 4, 0, 0, 1, 4, // Top face
				7, 5, 2, 2, 3, 5 // Bottom face
		});
		elements.rewind();

		// Create a VAO
		vaoID = glGenVertexArrays();
		glBindVertexArray(vaoID);

		// Create a VBO
		vboID = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vboID);
		glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		glBindBuffer(GL_ARRAY_BUFFER, 0);

		// Create a VBO for the colors
		vboTexID = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vboTexID);
		glBufferData(GL_ARRAY_BUFFER, textureCoords, GL_STATIC_DRAW);
		glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		//glBufferData(GL_ARRAY_BUFFER, colors, GL_STATIC_DRAW);
		//glVertexAttribPointer(1, 4, GL_FLOAT, false, 0, 0);
		glBindBuffer(GL_ARRAY_BUFFER, 0);

		// Create a EBO for indexed drawing
		eboID = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, elements, GL_STATIC_DRAW);
		

		texture = Texture.loadTexture("garndesh/openglrenderer/textures/test.png");
		texture.setActiveTextureUnit(0); 
		texture.bind();
	}

	public void RenderCube() {

//		// Bind the shaders
//		shader.bind();
//		shader.setUniform("m_model", location.getFloatBuffer());
//		shader.setUniform("m_view", camera.getViewBuffer());
//		shader.setUniform("m_proj", camera.getProjectionBuffer());
		// Bind the VAO
		// Bind the VAO
		glBindVertexArray(vaoID);
		glEnableVertexAttribArray(0);
		glEnableVertexAttribArray(1);
		// Draw a cube
		glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_SHORT, 0);
		// Unbind the VAO
		glDisableVertexAttribArray(0);
		glDisableVertexAttribArray(1);
		glBindVertexArray(0);
		// Unbind the shaders
		//ShaderProgram.unbind();
	}
	
	public void dispose(){
		// Dispose the VAO
		glBindVertexArray(0);
		glDeleteVertexArrays(vaoID);

		// Dispose the VBO
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glDeleteBuffers(vboID);
	}
}
