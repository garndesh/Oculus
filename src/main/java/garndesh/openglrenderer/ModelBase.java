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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL33;

public class ModelBase {
	private Texture texture;
	private int vaoID, vboID, vboTexID, eboID;
	private int elementCount;
	
	private ModelBase(FloatBuffer vertices, int vLength, FloatBuffer textureArray, int tLength, ShortBuffer elements, int elementLength, Texture texture){
		this.elementCount = elementLength;
		this.texture = texture;
		

		// Create a VAO
		vaoID = glGenVertexArrays();
		glBindVertexArray(vaoID);

		// Create a VBO
		vboID = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vboID);
		glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
		glVertexAttribPointer(0, vLength, GL_FLOAT, false, 0, 0);
		glBindBuffer(GL_ARRAY_BUFFER, 0);

		// Create a VBO for the colors
		vboTexID = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vboTexID);
		glBufferData(GL_ARRAY_BUFFER, textureArray, GL_STATIC_DRAW);
		glVertexAttribPointer(1, tLength, GL_FLOAT, false, 0, 0);
		glBindBuffer(GL_ARRAY_BUFFER, 0);

		// Create a EBO for indexed drawing
		eboID = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, elements, GL_STATIC_DRAW);

		// Unbind the VAO
		glBindVertexArray(0);

		this.texture.setActiveTextureUnit(0); 
		this.texture.bind();
	}
	
	
	public void renderModel(Transform location, ShaderProgram shader,
			Camera camera) {
		// Bind the shaders
				shader.bind();
				shader.setUniform("m_model", location.getFloatBuffer());
				shader.setUniform("m_view", camera.getViewBuffer());
				shader.setUniform("m_proj", camera.getProjectionBuffer());
				// Bind the VAO
				// Bind the VAO
				glBindVertexArray(vaoID);
				glEnableVertexAttribArray(0);
				glEnableVertexAttribArray(1);
				// Draw a cube
				glDrawElements(GL_TRIANGLES, elementCount, GL_UNSIGNED_SHORT, 0);
				// Unbind the VAO
				glDisableVertexAttribArray(0);
				glDisableVertexAttribArray(1);
				glBindVertexArray(0);
				// Unbind the shaders
				ShaderProgram.unbind();
	}
	
	

	public void dispose(){
		// Dispose the VAO
		glBindVertexArray(0);
		glDeleteVertexArrays(vaoID);

		// Dispose the VBO
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glDeleteBuffers(vboID);
	}
	
	
	//Model generation methods
	
	
	public static ModelBase generateModelFromFile(String fileName, String textureName){
		FloatBuffer vertices;
		FloatBuffer textureArray;
		ShortBuffer elements;
		
		List<Float> vList = new ArrayList<Float>();
		List<Float> tList = new ArrayList<Float>();
		List<Short[]> eList = new ArrayList<Short[]>();
		int lineNumber = 0;
		
		try {
			BufferedReader inputStream = new BufferedReader(new FileReader(fileName));
			String line;
			String[] parts;
			while(inputStream!= null && (line=inputStream.readLine())!=null){
				lineNumber++;
				parts = line.split(" ");
				switch(parts[0]){
				case "v":
					readFloats(parts, vList, 3);
					break;
				case "vt":
					readFloats(parts, tList, 2);
					break;
				case "f":
					readShorts(parts, eList);
					break;
				default:
					break;	
				}
			}
			inputStream.close();
		} catch (FileNotFoundException e) {
			System.err.println("Model "+fileName+" not found.");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Error on reading line in model file: "+fileName);
			e.printStackTrace();
		} catch (ModelFileExeption e) {
			System.err.println("ModelfileError on line "+lineNumber+" with errorcode "+e.error);
			e.printStackTrace();
		}
		
		vertices = BufferUtils.createFloatBuffer(vList.size());
		for(float i : vList){
			vertices.put(i);
		}
		vertices.rewind();
		
		textureArray = BufferUtils.createFloatBuffer(eList.size()*2);
		elements = BufferUtils.createShortBuffer(eList.size());
		for (Short[] i : eList){
			System.out.println(i[0]+"/"+i[1]);
			elements.put(i[0]);
			textureArray.put(tList.get(i[1]*2));
			textureArray.put(1-tList.get(i[1]*2+1));
			
			System.out.println(i[0]+" ("+tList.get(i[1]*2)+","+(1-tList.get(i[1]*2+1))+")");
			
		}
		textureArray.rewind();
		elements.rewind();
		
		return new ModelBase(vertices, 3, textureArray, 2, elements, eList.size(), Texture.loadTexture(textureName));
	}
	
	
	private static void readFloats(String[] parts, List<Float> fList, int expectedCount) throws ModelFileExeption{
		int length = parts.length;
		float tmpValue;
		if(length!=expectedCount+1)
			throw new ModelFileExeption(ModelFileExeption.INPUT_INCORRECT_LENGTH);
		for(int i = 1; i<length; i++){
			tmpValue = Float.valueOf(parts[i]);
			if(tmpValue == Float.NaN)
				throw new ModelFileExeption(ModelFileExeption.INPUT_CANNOT_PARSE);
			fList.add(tmpValue);
			System.out.print(tmpValue+" ");
		}
		System.out.println();
	}
	
	private static void readShorts(String[] parts, List<Short[]> sList) throws ModelFileExeption{
		int length = parts.length-1;
		if(length<3)
			throw new ModelFileExeption(ModelFileExeption.INPUT_TO_SHORT);
		if(length>4)
			throw new ModelFileExeption(ModelFileExeption.INPUT_TO_LONG);
		for (int i = 0; i<3; i++){
			Short[] tmpValue = new Short[2];
			String[] values = parts[i+1].split("/");
			tmpValue[0] = (short) (Short.parseShort(values[0])-1);
			tmpValue[1] = (short) (Short.parseShort(values[1])-1);
			sList.add(tmpValue);
			System.out.print(tmpValue[0]+"/"+tmpValue[1]+" ");
		}
		if(length == 4){
			for(int i : new int[]{2, 3, 1}){
				Short[] tmpValue = new Short[2];
				String[] values = parts[i+1].split("/");
				tmpValue[0] = (short) (Short.parseShort(values[0])-1);
				tmpValue[1] = (short) (Short.parseShort(values[1])-1);
				sList.add(tmpValue);
				System.out.print(tmpValue[0]+"/"+tmpValue[1]+" ");
			}
		}
		System.out.println();
		
		
	}
	
}
