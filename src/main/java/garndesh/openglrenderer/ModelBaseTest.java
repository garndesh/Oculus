package garndesh.openglrenderer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;

public class ModelBaseTest {

	private Texture texture;
	private int vaoID, vboID, eboID;
	boolean textured, normals;
	private int elementCount;

	public ModelBaseTest(FloatBuffer vertices, ShortBuffer elements,
			boolean tex, boolean norm, Texture texture) {

		System.out.println("Creating modelBaseTest");
		this.texture = texture;
		this.textured = tex;
		this.normals = norm;
		this.elementCount = elements.limit();
		int vLength = 3 + (tex ? 2 : 0) + (norm ? 3 : 0);
		System.out.println("vLength is: " + vLength);
		// Create a VAO
		vaoID = glGenVertexArrays();
		glBindVertexArray(vaoID);
		System.out.println("Starting buffer creation");
		// Create a VBO
		vboID = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vboID);
		glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, vLength * Float.BYTES, 0);

		if (tex) {
			glVertexAttribPointer(1, 2, GL_FLOAT, false, vLength * Float.BYTES,
					3 * Float.BYTES);
		}
		if (norm) {
			glVertexAttribPointer(2, 3, GL_FLOAT, false, vLength * Float.BYTES,
					((tex ? 2 : 0) + 3) * Float.BYTES);

		}
		

		glBindBuffer(GL_ARRAY_BUFFER, 0);
		
		
		// Create a EBO for indexed drawing
		eboID = glGenBuffers();
		glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboID);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, elements, GL_STATIC_DRAW);

		// Unbind the VAO
		glBindVertexArray(0);

		System.out.println("Buffers Created, binding texture");

		this.texture.setActiveTextureUnit(0);
		this.texture.bind();
		System.out.println("texture bound");
	}

	public void renderModel(Transform location, ShaderProgram shader,
			ACamera camera) {

		shader.setUniform("m_model", location.getFloatBuffer());
		// Bind the VAO
		// Bind the VAO
		glBindVertexArray(vaoID);
		glEnableVertexAttribArray(0);
		if (textured) {
			//glBindVertexArray(tboID);
			glEnableVertexAttribArray(1);
		}
		if (normals) {
			//glBindVertexArray(nboID);
			glEnableVertexAttribArray(2);
		}
		// Draw a cube
		glDrawElements(GL_TRIANGLES, elementCount, GL_UNSIGNED_SHORT, 0);
		// Unbind the VAO
		glEnableVertexAttribArray(0);
		if (textured)
			glDisableVertexAttribArray(1);
		if (normals)
			glDisableVertexAttribArray(2);
		glBindVertexArray(0);
		// Unbind the shaders
		ShaderProgram.unbind();
	}

	public void dispose() {
		// Dispose the VAO
		glBindVertexArray(0);
		glDeleteVertexArrays(vaoID);

		// Dispose the VBO
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glDeleteBuffers(vboID);
	}

	public static HashMap<String, ModelBaseTest> generateModelsFromFile(String fileName,
			String textureName) {
		boolean textured = false;
		boolean normals = false;

		HashMap<String, ModelArrays> modelObjects = new HashMap<String, ModelArrays>();
		int lineNumber = 0;
		

		List<Float> vList = new ArrayList<Float>();
		List<Float> tList = new ArrayList<Float>();
		List<Float> nList = new ArrayList<Float>();

		try {
			BufferedReader inputStream = new BufferedReader(new FileReader(fileName));
			String line;
			String currentModel = "";
			String[] parts;

			while (inputStream != null
					&& (line = inputStream.readLine()) != null) {
				lineNumber++;
				parts = line.split(" ");
				switch (parts[0]) {
				case "o":
					modelObjects.put(parts[1], new ModelArrays());
					currentModel = parts[1];
					break;
				case "v":
					if(currentModel == "")
						break;
					readFloats(parts, vList, 3);
					break;
				case "vt":
					if(currentModel == "")
						break;
					readFloats(parts, tList, 2);
					break;
				case "vn":
					if(currentModel == "")
						break;
					readFloats(parts, nList, 3);
					break;
				case "f":
					if(currentModel == "")
						break;
					// readShorts(parts, eList);
					for (int i = 1; i < 4; i++) {
						modelObjects.get(currentModel).eList.add(parts[i]);
					} 
					if(parts.length==5){
						for (int i : new int[]{1, 3, 4}){
							modelObjects.get(currentModel).eList.add(parts[i]);
						}
					}
					break;
				default:
					break;
				}
			}
			inputStream.close();
		} catch (FileNotFoundException e) {
			System.err.println("Model " + fileName + " not found.");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Error on reading line in model file: "
					+ fileName);
			e.printStackTrace();
		} catch (ModelFileExeption e) {
			System.err.println("ModelfileError on line " + lineNumber
					+ " with errorcode " + e.error);
			e.printStackTrace();
		}

		HashMap<String, ModelBaseTest> models = new HashMap<String, ModelBaseTest>();
		Iterator<Entry<String, ModelArrays>> iter =  modelObjects.entrySet().iterator();
		FloatBuffer vertices;
		ShortBuffer elements;
		while (iter.hasNext()){
			System.out.println("Starting with new Object");
			
			textured = false;
			normals = false;
			Entry<String, ModelArrays> entry  = (Entry<String, ModelArrays>) iter.next();
			ModelArrays array = entry.getValue();
			//System.out.println(array.eList.size());
			//System.out.println(vList.size());
	
			List<String> map = new ArrayList<String>(new LinkedHashSet<String>(array.eList));
			//System.out.println(map.size());
	
			String[] tmp = map.get(0).split("/");
			//System.out.println("tex try = "+map.get(0));
			if (tmp.length > 1 && tmp[1] != "") {
				textured = true;
			}
			if (tmp.length > 2 && tmp[2] != "") {
				normals = true;
			}

			vertices = BufferUtils.createFloatBuffer(map.size()
					* (3 + (textured ? 2 : 0) + (normals ? 3 : 0)));
			for (String i : map) {
				String[] indecies = i.split("/");
				short vertex = (short) (Short.valueOf(indecies[0]) - 1);
				//System.out.println(i + " " + indecies.length);
				vertices.put(vList.get(vertex * 3));
				vertices.put(vList.get(vertex * 3 + 1));
				vertices.put(vList.get(vertex * 3 + 2));
				if (textured) {
					short texture = (short) (Short.valueOf(indecies[1]) - 1);
					//System.out.println("Getting texture coords: " + texture);
					vertices.put(tList.get(texture * 2));
					vertices.put(1-tList.get(texture * 2 + 1));
				}
				if (normals) {
					short normal = (short) (Short.valueOf(indecies[2]) - 1);
					vertices.put(tList.get(normal * 3));
					vertices.put(tList.get(normal * 3 + 1));
					vertices.put(tList.get(normal * 3 + 2));
				}
			}
			vertices.rewind();
	
			elements = BufferUtils.createShortBuffer(array.eList.size());
			System.out.println("Element Buffer");
			for (String i : array.eList) {
				// System.out.println(i);
				short index = (short) map.indexOf(i);
				elements.put(index);
				//System.out.println(i + " "+map.indexOf(i)+"( "+vertices.get(index*(3+(textured?2:0)+(normals?3:0)))+","+vertices.get(index*(3+(textured?2:0)+(normals?3:0))+1)+","+vertices.get(index*(3+(textured?2:0)+(normals?3:0))+2)+" ) ,"+vertices.get(index*(3+(textured?2:0)+(normals?3:0))+3)+","+vertices.get(index*(3+(textured?2:0)+(normals?3:0))+4));
				// System.out.println(i[0]+" ("+tList.get(i[1]*2)+","+(1-tList.get(i[1]*2+1))+")");
	
			}
			elements.rewind();
			
			models.put(entry.getKey(), new ModelBaseTest(vertices.duplicate(), elements.duplicate(), textured, normals,
					Texture.loadTexture(textureName)));
			vertices.clear();
			elements.clear();
		}
		// return null;
		return models;
	}

	private static void readFloats(String[] parts, List<Float> fList,
			int expectedCount) throws ModelFileExeption {
		int length = parts.length;
		float tmpValue;
		if (length != expectedCount + 1)
			throw new ModelFileExeption(
					ModelFileExeption.INPUT_INCORRECT_LENGTH);
		for (int i = 1; i < length; i++) {
			tmpValue = Float.valueOf(parts[i]);
			if (tmpValue == Float.NaN)
				throw new ModelFileExeption(
						ModelFileExeption.INPUT_CANNOT_PARSE);
			fList.add(tmpValue);
			//System.out.print(tmpValue + " ");
		}
		//System.out.println();
	}

}
