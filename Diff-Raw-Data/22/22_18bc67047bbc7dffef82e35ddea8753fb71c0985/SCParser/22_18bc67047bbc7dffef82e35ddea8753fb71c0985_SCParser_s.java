 package org.sunflow.core.parser;
 
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.io.StringReader;
 
 import org.codehaus.janino.ClassBodyEvaluator;
 import org.codehaus.janino.CompileException;
 import org.codehaus.janino.Scanner;
 import org.codehaus.janino.Parser.ParseException;
 import org.codehaus.janino.Scanner.ScanException;
 import org.sunflow.SunflowAPI;
 import org.sunflow.core.CausticPhotonMapInterface;
 import org.sunflow.core.GlobalPhotonMapInterface;
 import org.sunflow.core.SceneParser;
 import org.sunflow.core.Shader;
 import org.sunflow.core.Tesselatable;
 import org.sunflow.core.camera.PinholeCamera;
 import org.sunflow.core.camera.SphericalCamera;
 import org.sunflow.core.camera.ThinLensCamera;
 import org.sunflow.core.filter.BlackmanHarrisFilter;
 import org.sunflow.core.filter.BoxFilter;
 import org.sunflow.core.filter.GaussianFilter;
 import org.sunflow.core.filter.SincFilter;
 import org.sunflow.core.filter.TriangleFilter;
 import org.sunflow.core.gi.AmbientOcclusionGIEngine;
 import org.sunflow.core.gi.FakeGIEngine;
 import org.sunflow.core.gi.InstantGI;
 import org.sunflow.core.gi.IrradianceCacheGIEngine;
 import org.sunflow.core.gi.PathTracingGIEngine;
 import org.sunflow.core.light.DirectionalSpotlight;
 import org.sunflow.core.light.ImageBasedLight;
 import org.sunflow.core.light.MeshLight;
 import org.sunflow.core.photonmap.CausticPhotonMap;
 import org.sunflow.core.photonmap.GlobalPhotonMap;
 import org.sunflow.core.photonmap.GridPhotonMap;
 import org.sunflow.core.primitive.Background;
 import org.sunflow.core.primitive.BanchoffSurface;
 import org.sunflow.core.primitive.CornellBox;
 import org.sunflow.core.primitive.Hair;
 import org.sunflow.core.primitive.Mesh;
 import org.sunflow.core.primitive.Plane;
 import org.sunflow.core.primitive.Sphere;
 import org.sunflow.core.primitive.Torus;
 import org.sunflow.core.shader.AmbientOcclusionShader;
 import org.sunflow.core.shader.AnisotropicWardShader;
 import org.sunflow.core.shader.ConstantShader;
 import org.sunflow.core.shader.DiffuseShader;
 import org.sunflow.core.shader.GlassShader;
 import org.sunflow.core.shader.IDShader;
 import org.sunflow.core.shader.MirrorShader;
 import org.sunflow.core.shader.PhongShader;
 import org.sunflow.core.shader.ShinyDiffuseShader;
 import org.sunflow.core.shader.TexturedAmbientOcclusionShader;
 import org.sunflow.core.shader.TexturedDiffuseShader;
 import org.sunflow.core.shader.TexturedPhongShader;
 import org.sunflow.core.shader.TexturedShinyDiffuseShader;
 import org.sunflow.core.shader.TexturedWardShader;
 import org.sunflow.core.shader.UberShader;
 import org.sunflow.core.shader.ViewCausticsShader;
 import org.sunflow.core.shader.ViewGlobalPhotonsShader;
 import org.sunflow.core.shader.ViewIrradianceShader;
 import org.sunflow.core.tesselatable.Gumbo;
 import org.sunflow.core.tesselatable.Teapot;
 import org.sunflow.image.Color;
 import org.sunflow.math.Matrix4;
 import org.sunflow.math.Point3;
 import org.sunflow.math.Vector3;
 import org.sunflow.system.Parser;
 import org.sunflow.system.Timer;
 import org.sunflow.system.UI;
 import org.sunflow.system.Parser.ParserException;
 
 /**
  * This class provides a static method for loading files in the Sunflow scene
  * file format.
  */
 public class SCParser implements SceneParser {
     private Parser p;
     private int numLightSamples;
 
     public SCParser() {
     }
 
     public boolean parse(String filename, SunflowAPI api) {
         String localDir = new File(filename).getAbsoluteFile().getParentFile().getAbsolutePath();
         numLightSamples = 1;
         Timer timer = new Timer();
         timer.start();
         UI.printInfo("[API] Parsing \"%s\" ...", filename);
         try {
             p = new Parser(filename);
             while (true) {
                 String token = p.getNextToken();
                 if (token == null)
                     break;
                 if (token.equals("image")) {
                     UI.printInfo("[API] Reading image settings ...");
                     parseImageBlock(api);
                 } else if (token.equals("background")) {
                     UI.printInfo("[API] Reading background ...");
                     parseBackgroundBlock(api);
                 } else if (token.equals("accel")) {
                     UI.printInfo("[API] Reading accelerator type ...");
                     api.accel(p.getNextToken());
                 } else if (token.equals("filter")) {
                     UI.printInfo("[API] Reading image filter type ...");
                     parseFilter(api);
                 } else if (token.equals("bucket")) {
                     UI.printInfo("[API] Reading bucket settings ...");
                     api.bucketSize(p.getNextInt());
                     api.bucketOrder(p.getNextToken());
                 } else if (token.equals("photons")) {
                     UI.printInfo("[API] Reading photon settings ...");
                     parsePhotonBlock(api);
                 } else if (token.equals("gi")) {
                     UI.printInfo("[API] Reading global illumination settings ...");
                     parseGIBlock(api);
                 } else if (token.equals("lightserver")) {
                     UI.printInfo("[API] Reading light server settings ...");
                     parseLightserverBlock(api);
                 } else if (token.equals("trace-depths")) {
                     UI.printInfo("[API] Reading trace depths ...");
                     parseTraceBlock(api);
                 } else if (token.equals("camera")) {
                     UI.printInfo("[API] Reading camera ...");
                     parseCamera(api);
                 } else if (token.equals("shader")) {
                     if (!parseShader(api))
                         return false;
                 } else if (token.equals("override")) {
                     api.shaderOverride(p.getNextToken(), p.getNextBoolean());
                 } else if (token.equals("object")) {
                     parseObjectBlock(api);
                 } else if (token.equals("instance")) {
                     parseInstanceBlock(api);
                 } else if (token.equals("light")) {
                     parseLightBlock(api);
                 } else if (token.equals("texturepath")) {
                     String path = p.getNextToken();
                     if (!new File(path).isAbsolute())
                         path = localDir + File.separator + path;
                     api.addTextureSearchPath(path);
                 } else if (token.equals("includepath")) {
                     String path = p.getNextToken();
                     if (!new File(path).isAbsolute())
                         path = localDir + File.separator + path;
                     api.addIncludeSearchPath(path);
                 } else if (token.equals("include")) {
                     String file = p.getNextToken();
                     UI.printInfo("[API] Including: \"%s\" ...", file);
                     api.parse(file);
                 } else
                     UI.printWarning("[API] Unrecognized token %s", token);
             }
             p.close();
         } catch (ParserException e) {
             UI.printError("[API] %s", e.getMessage());
             e.printStackTrace();
             return false;
         } catch (FileNotFoundException e) {
             UI.printError("[API] %s", e.getMessage());
             return false;
         } catch (IOException e) {
             UI.printError("[API] %s", e.getMessage());
             return false;
         }
         timer.end();
         UI.printInfo("[API] Done parsing.");
         UI.printInfo("[API] Parsing time: %s", timer.toString());
         return true;
     }
 
     private void parseImageBlock(SunflowAPI api) throws IOException, ParserException {
         p.checkNextToken("{");
         p.checkNextToken("resolution");
         api.resolution(p.getNextInt(), p.getNextInt());
         p.checkNextToken("aa");
         int min = p.getNextInt();
         int max = p.getNextInt();
         api.antiAliasing(min, max);
         if (p.peekNextToken("samples")) {
             int n = p.getNextInt();
             api.antiAliasing(min, max, n);
         }
         if (p.peekNextToken("show-aa")) {
             UI.printWarning("[API] Deprecated: show-aa ignored");
             p.getNextBoolean();
         }
         if (p.peekNextToken("output")) {
             UI.printWarning("[API] Deprecated: output statement ignored");
             p.getNextToken();
         }
         p.checkNextToken("}");
     }
 
     private void parseBackgroundBlock(SunflowAPI api) throws IOException, ParserException {
         p.checkNextToken("{");
         p.checkNextToken("color");
         api.parameter("color", parseColor());
         api.shader("background.shader", new ConstantShader());
         api.geometry("background", new Background());
         api.parameter("shaders", "background.shader");
         api.instance("background.instance", "background");
         p.checkNextToken("}");
     }
 
     private void parseFilter(SunflowAPI api) throws IOException, ParserException {
         if (p.peekNextToken("box")) {
             float w = p.getNextFloat();
             float h = p.getNextFloat();
             float s = (w + h) * 0.5f;
             api.filter(new BoxFilter(s));
         } else if (p.peekNextToken("gaussian")) {
             float w = p.getNextFloat();
             float h = p.getNextFloat();
             float s = (w + h) * 0.5f;
             api.filter(new GaussianFilter(s));
         } else if (p.peekNextToken("blackman-harris")) {
             float w = p.getNextFloat();
             float h = p.getNextFloat();
             float s = (w + h) * 0.5f;
             api.filter(new BlackmanHarrisFilter(s));
         } else if (p.peekNextToken("sinc")) {
             float w = p.getNextFloat();
             float h = p.getNextFloat();
             float s = (w + h) * 0.5f;
             api.filter(new SincFilter(s));
         } else if (p.peekNextToken("triangle")) {
             float w = p.getNextFloat();
             float h = p.getNextFloat();
             float s = (w + h) * 0.5f;
             api.filter(new TriangleFilter(s));
         } else
             api.filter(p.getNextToken());
     }
 
     private void parsePhotonBlock(SunflowAPI api) throws ParserException, IOException {
         String type;
         int gather;
         float radius;
         int numEmit = 0;
         boolean globalEmit = false;
         p.checkNextToken("{");
         if (p.peekNextToken("emit")) {
             UI.printWarning("[API] Shared photon emit values are deprectated - specify number of photons to emit per map");
             numEmit = p.getNextInt();
             globalEmit = true;
         }
         if (p.peekNextToken("global")) {
             UI.printWarning("[API] Global photon map setting belonds inside the gi block - ignoring");
             if (!globalEmit)
                 p.getNextInt();
             p.getNextToken();
             p.getNextInt();
             p.getNextFloat();
         }
         p.checkNextToken("caustics");
         if (!globalEmit)
             numEmit = p.getNextInt();
         type = p.getNextToken();
         gather = p.getNextInt();
         radius = p.getNextFloat();
         CausticPhotonMapInterface cmap = null;
         if (type.equals("kd"))
             cmap = new CausticPhotonMap(numEmit, gather, radius, 1.1f);
         else if (type.equals("none"))
             cmap = null;
         else
             UI.printWarning("[API] Unrecognized caustic photon map type: %s", type);
         api.photons(cmap);
         p.checkNextToken("}");
     }
 
     private void parseGIBlock(SunflowAPI api) throws ParserException, IOException {
         p.checkNextToken("{");
         p.checkNextToken("type");
         if (p.peekNextToken("irr-cache")) {
             p.checkNextToken("samples");
             int samples = p.getNextInt();
             p.checkNextToken("tolerance");
             float tolerance = p.getNextFloat();
             p.checkNextToken("spacing");
             float min = p.getNextFloat();
             float max = p.getNextFloat();
             // parse global photon map info
             GlobalPhotonMapInterface gmap = null;
             if (p.peekNextToken("global")) {
                 int numEmit = p.getNextInt();
                 String type = p.getNextToken();
                 int gather = p.getNextInt();
                 float radius = p.getNextFloat();
                 if (type.equals("kd"))
                     gmap = new GlobalPhotonMap(numEmit, gather, radius);
                 else if (type.equals("grid"))
                     gmap = new GridPhotonMap(numEmit, gather, radius);
                 else if (type.equals("none"))
                     gmap = null;
                 else
                     UI.printWarning("[API] Unrecognized global photon map type: %s", type);
             }
             api.giEngine(new IrradianceCacheGIEngine(samples, tolerance, min, max, gmap));
         } else if (p.peekNextToken("path")) {
             p.checkNextToken("samples");
             int samples = p.getNextInt();
             if (p.peekNextToken("bounces")) {
                 UI.printWarning("[API] Deprecated setting: bounces - use diffuse trace depth instead");
                 p.getNextInt();
             }
             api.giEngine(new PathTracingGIEngine(samples));
         } else if (p.peekNextToken("fake")) {
             p.checkNextToken("up");
             Vector3 up = parseVector();
             p.checkNextToken("sky");
             Color sky = parseColor();
             p.checkNextToken("ground");
             Color ground = parseColor();
             api.giEngine(new FakeGIEngine(up, sky, ground));
         } else if (p.peekNextToken("igi")) {
             p.checkNextToken("samples");
             int samples = p.getNextInt();
             p.checkNextToken("sets");
             int sets = p.getNextInt();
             p.checkNextToken("b");
             float b = p.getNextFloat();
             p.checkNextToken("bias-samples");
             int bias = p.getNextInt();
             api.giEngine(new InstantGI(samples, sets, b, bias));
         } else if (p.peekNextToken("ambocc")) {
             p.checkNextToken("bright");
             Color bright = parseColor();
             p.checkNextToken("dark");
             Color dark = parseColor();
             p.checkNextToken("samples");
             int samples = p.getNextInt();
             float maxdist = 0;
             if (p.peekNextToken("maxdist"))
                 maxdist = p.getNextFloat();
             api.giEngine(new AmbientOcclusionGIEngine(bright, dark, samples, maxdist));
         } else if (p.peekNextToken("none") || p.peekNextToken("null")) {
             // disable GI
             api.giEngine(null);
         } else
             UI.printWarning("[API] Unrecognized gi engine type: %s", p.getNextToken());
         p.checkNextToken("}");
     }
 
     private void parseLightserverBlock(SunflowAPI api) throws ParserException, IOException {
         p.checkNextToken("{");
         if (p.peekNextToken("shadows")) {
             UI.printWarning("[API] Deprecated: shadows setting ignored");
             p.getNextBoolean();
         }
         if (p.peekNextToken("direct-samples")) {
             UI.printWarning("[API] Deprecated: use samples keyword in area light definitions");
             numLightSamples = p.getNextInt();
         }
         if (p.peekNextToken("glossy-samples")) {
             UI.printWarning("[API] Deprecated: use samples keyword in glossy shader definitions");
             p.getNextInt();
         }
         if (p.peekNextToken("max-depth")) {
             UI.printWarning("[API] Deprecated: max-depth setting - use trace-depths block instead");
             int d = p.getNextInt();
             api.traceDepth(1, d - 1, 0);
         }
         if (p.peekNextToken("global")) {
             UI.printWarning("[API] Deprecated: global settings ignored - use photons block instead");
             p.getNextBoolean();
             p.getNextInt();
             p.getNextInt();
             p.getNextInt();
             p.getNextFloat();
         }
         if (p.peekNextToken("caustics")) {
             UI.printWarning("[API] Deprecated: caustics settings ignored - use photons block instead");
             p.getNextBoolean();
             p.getNextInt();
             p.getNextFloat();
             p.getNextInt();
             p.getNextFloat();
         }
         if (p.peekNextToken("irr-cache")) {
             UI.printWarning("[API] Deprecated: irradiance cache settings ignored - use gi block instead");
             p.getNextInt();
             p.getNextFloat();
             p.getNextFloat();
             p.getNextFloat();
         }
         p.checkNextToken("}");
     }
 
     private void parseTraceBlock(SunflowAPI api) throws ParserException, IOException {
         int diff = 0, refl = 0, refr = 0;
         p.checkNextToken("{");
         if (p.peekNextToken("diff"))
             diff = p.getNextInt();
         if (p.peekNextToken("refl"))
             refl = p.getNextInt();
         if (p.peekNextToken("refr"))
             refr = p.getNextInt();
         p.checkNextToken("}");
         api.traceDepth(diff, refl, refr);
     }
 
     private void parseCamera(SunflowAPI api) throws ParserException, IOException {
         p.checkNextToken("{");
         p.checkNextToken("type");
         if (p.peekNextToken("pinhole")) {
             UI.printInfo("[API] Reading pinhole camera ...");
             p.checkNextToken("eye");
             Point3 eye = parsePoint();
             p.checkNextToken("target");
             Point3 target = parsePoint();
             p.checkNextToken("up");
             Vector3 up = parseVector();
             p.checkNextToken("fov");
             float fov = p.getNextFloat();
             p.checkNextToken("aspect");
             float aspect = p.getNextFloat();
             api.camera(new PinholeCamera(eye, target, up, fov, aspect));
         } else if (p.peekNextToken("thinlens")) {
             UI.printInfo("[API] Reading thinlens camera ...");
             p.checkNextToken("eye");
             Point3 eye = parsePoint();
             p.checkNextToken("target");
             Point3 target = parsePoint();
             p.checkNextToken("up");
             Vector3 up = parseVector();
             p.checkNextToken("fov");
             float fov = p.getNextFloat();
             p.checkNextToken("aspect");
             float aspect = p.getNextFloat();
             p.checkNextToken("fdist");
             float fdist = p.getNextFloat();
             p.checkNextToken("lensr");
             float lensr = p.getNextFloat();
             api.camera(new ThinLensCamera(eye, target, up, fov, aspect, fdist, lensr));
         } else if (p.peekNextToken("spherical")) {
             UI.printInfo("[API] Reading spherical camera ...");
             p.checkNextToken("eye");
             Point3 eye = parsePoint();
             p.checkNextToken("target");
             Point3 target = parsePoint();
             p.checkNextToken("up");
             Vector3 up = parseVector();
             api.camera(new SphericalCamera(eye, target, up));
         } else
             UI.printWarning("[API] Unrecognized camera type: %s", p.getNextToken());
         p.checkNextToken("}");
     }
 
     private boolean parseShader(SunflowAPI api) throws ParserException, IOException {
         p.checkNextToken("{");
         p.checkNextToken("name");
         String name = p.getNextToken();
         UI.printInfo("[API] Reading shader: %s ...", name);
         p.checkNextToken("type");
         if (p.peekNextToken("diffuse")) {
             if (p.peekNextToken("diff")) {
                 api.parameter("diffuse", parseColor());
                 api.shader(name, new DiffuseShader());
             } else if (p.peekNextToken("texture")) {
                 api.parameter("texture", p.getNextToken());
                 api.shader(name, new TexturedDiffuseShader());
             } else
                 UI.printWarning("[API] Unrecognized option in diffuse shader block: %s", p.getNextToken());
         } else if (p.peekNextToken("phong")) {
             String tex = null;
             if (p.peekNextToken("texture"))
                 api.parameter("texture", tex = p.getNextToken());
             else {
                 p.checkNextToken("diff");
                 api.parameter("diffuse", parseColor());
             }
             p.checkNextToken("spec");
             api.parameter("specular", parseColor());
             api.parameter("power", p.getNextFloat());
             if (p.peekNextToken("samples"))
                 api.parameter("samples", p.getNextInt());
             if (tex != null)
                 api.shader(name, new TexturedPhongShader());
             else
                 api.shader(name, new PhongShader());
         } else if (p.peekNextToken("amb-occ") || p.peekNextToken("amb-occ2")) {
             String tex = null;
             if (p.peekNextToken("diff") || p.peekNextToken("bright"))
                 api.parameter("bright", parseColor());
             else if (p.peekNextToken("texture"))
                 api.parameter("texture", tex = p.getNextToken());
             if (p.peekNextToken("dark")) {
                 api.parameter("dark", parseColor());
                 p.checkNextToken("samples");
                 api.parameter("samples", p.getNextInt());
                 p.checkNextToken("dist");
                 api.parameter("maxdist", p.getNextFloat());
             }
             if (tex == null)
                 api.shader(name, new AmbientOcclusionShader());
             else
                 api.shader(name, new TexturedAmbientOcclusionShader());
         } else if (p.peekNextToken("mirror")) {
             p.checkNextToken("refl");
             api.parameter("color", parseColor());
             api.shader(name, new MirrorShader());
         } else if (p.peekNextToken("glass")) {
             p.checkNextToken("eta");
             api.parameter("eta", p.getNextFloat());
             p.checkNextToken("color");
             api.parameter("color", parseColor());
             api.shader(name, new GlassShader());
         } else if (p.peekNextToken("shiny")) {
             String tex = null;
             if (p.peekNextToken("texture"))
                 api.parameter("texture", tex = p.getNextToken());
             else {
                 p.checkNextToken("diff");
                 api.parameter("diffuse", parseColor());
             }
             p.checkNextToken("refl");
             api.parameter("shiny", p.getNextFloat());
             if (tex == null)
                 api.shader(name, new ShinyDiffuseShader());
             else
                 api.shader(name, new TexturedShinyDiffuseShader());
         } else if (p.peekNextToken("ward")) {
             String tex = null;
             if (p.peekNextToken("texture"))
                 api.parameter("texture", tex = p.getNextToken());
             else {
                 p.checkNextToken("diff");
                 api.parameter("diffuse", parseColor());
             }
             p.checkNextToken("spec");
             api.parameter("specular", parseColor());
             p.checkNextToken("rough");
             api.parameter("roughnessX", p.getNextFloat());
             api.parameter("roughnessY", p.getNextFloat());
             if (p.peekNextToken("samples"))
                 api.parameter("samples", p.getNextInt());
             if (tex != null)
                 api.shader(name, new TexturedWardShader());
             else
                 api.shader(name, new AnisotropicWardShader());
         } else if (p.peekNextToken("view-caustics")) {
             api.shader(name, new ViewCausticsShader());
         } else if (p.peekNextToken("view-irradiance")) {
             api.shader(name, new ViewIrradianceShader());
         } else if (p.peekNextToken("view-global")) {
             api.shader(name, new ViewGlobalPhotonsShader());
         } else if (p.peekNextToken("constant")) {
             // backwards compatibility -- peek only
             p.peekNextToken("color");
             api.parameter("color", parseColor());
             api.shader(name, new ConstantShader());
         } else if (p.peekNextToken("janino")) {
             String code = p.getNextCodeBlock();
             try {
                 Shader shader = (Shader) ClassBodyEvaluator.createFastClassBodyEvaluator(new Scanner(null, new StringReader(code)), Shader.class, ClassLoader.getSystemClassLoader());
                 api.shader(name, shader);
             } catch (CompileException e) {
                 UI.printDetailed("[API] Compiling: %s", code);
                 UI.printError("[API] %s", e.getMessage());
                 e.printStackTrace();
                 return false;
             } catch (ParseException e) {
                 UI.printDetailed("[API] Compiling: %s", code);
                 UI.printError("[API] %s", e.getMessage());
                 e.printStackTrace();
                 return false;
             } catch (ScanException e) {
                 UI.printDetailed("[API] Compiling: %s", code);
                 UI.printError("[API] %s", e.getMessage());
                 e.printStackTrace();
                 return false;
             } catch (IOException e) {
                 UI.printDetailed("[API] Compiling: %s", code);
                 UI.printError("[API] %s", e.getMessage());
                 e.printStackTrace();
                 return false;
             }
         } else if (p.peekNextToken("id")) {
             api.shader(name, new IDShader());
         } else if (p.peekNextToken("uber")) {
             p.checkNextToken("diff");
             api.parameter("diffuse", parseColor());
             p.checkNextToken("refl");
             api.parameter("reflection", parseColor());
             if (p.peekNextToken("texture")) {
                 api.parameter("texture", p.getNextToken());
                 api.parameter("blend", p.getNextFloat());
             }
             api.shader(name, new UberShader());
         } else
             UI.printWarning("[API] Unrecognized shader type: %s", p.getNextToken());
         p.checkNextToken("}");
         return true;
     }
 
     private void parseObjectBlock(SunflowAPI api) throws ParserException, IOException {
         p.checkNextToken("{");
         String[] shaders = null;
         boolean multiShader = false;
         if (p.peekNextToken("shaders")) {
             int n = p.getNextInt();
             shaders = new String[n];
             for (int i = 0; i < n; i++)
                 shaders[i] = p.getNextToken();
             multiShader = true;
         } else {
             p.checkNextToken("shader");
             shaders = new String[] { p.getNextToken() };
         }
         Matrix4 transform = null;
         if (p.peekNextToken("transform"))
             transform = parseMatrix();
         p.checkNextToken("type");
         if (p.peekNextToken("mesh")) {
             UI.printWarning("[API] Deprecated object type: mesh");
             p.checkNextToken("name");
             String name = p.getNextToken();
             UI.printInfo("[API] Reading mesh: %s ...", name);
             int numVertices = p.getNextInt();
             int numTriangles = p.getNextInt();
             float[] points = new float[numVertices * 3];
             float[] normals = new float[numVertices * 3];
             float[] uvs = new float[numVertices * 2];
             for (int i = 0; i < numVertices; i++) {
                 p.checkNextToken("v");
                 points[3 * i + 0] = p.getNextFloat();
                 points[3 * i + 1] = p.getNextFloat();
                 points[3 * i + 2] = p.getNextFloat();
                 normals[3 * i + 0] = p.getNextFloat();
                 normals[3 * i + 1] = p.getNextFloat();
                 normals[3 * i + 2] = p.getNextFloat();
                 uvs[2 * i + 0] = p.getNextFloat();
                 uvs[2 * i + 1] = p.getNextFloat();
             }
             int[] triangles = new int[numTriangles * 3];
             for (int i = 0; i < numTriangles; i++) {
                 p.checkNextToken("t");
                 triangles[i * 3 + 0] = p.getNextInt();
                 triangles[i * 3 + 1] = p.getNextInt();
                 triangles[i * 3 + 2] = p.getNextInt();
             }
             // create geometry
             api.parameter("triangles", triangles);
             api.parameter("points", "point", "vertex", points);
             api.parameter("normals", "vector", "vertex", normals);
             api.parameter("uvs", "texcoord", "vertex", uvs);
             api.geometry(name, new Mesh());
             // create instance
             api.parameter("shaders", shaders);
             if (transform != null)
                 api.parameter("transform", transform);
             api.instance(name + ".instance", name);
         } else if (p.peekNextToken("flat-mesh")) {
             UI.printWarning("[API] Deprecated object type: flat-mesh");
             p.checkNextToken("name");
             String name = p.getNextToken();
             UI.printInfo("[API] Reading flat mesh: %s ...", name);
             int numVertices = p.getNextInt();
             int numTriangles = p.getNextInt();
             float[] points = new float[numVertices * 3];
             float[] uvs = new float[numVertices * 2];
             for (int i = 0; i < numVertices; i++) {
                 p.checkNextToken("v");
                 points[3 * i + 0] = p.getNextFloat();
                 points[3 * i + 1] = p.getNextFloat();
                 points[3 * i + 2] = p.getNextFloat();
                 p.getNextFloat();
                 p.getNextFloat();
                 p.getNextFloat();
                 uvs[2 * i + 0] = p.getNextFloat();
                 uvs[2 * i + 1] = p.getNextFloat();
             }
             int[] triangles = new int[numTriangles * 3];
             for (int i = 0; i < numTriangles; i++) {
                 p.checkNextToken("t");
                 triangles[i * 3 + 0] = p.getNextInt();
                 triangles[i * 3 + 1] = p.getNextInt();
                 triangles[i * 3 + 2] = p.getNextInt();
             }
 
             // create geometry
             api.parameter("triangles", triangles);
             api.parameter("points", "point", "vertex", points);
             api.parameter("uvs", "texcoord", "vertex", uvs);
             api.geometry(name, new Mesh());
             // create instance
             api.parameter("shaders", shaders);
             if (transform != null)
                 api.parameter("transform", transform);
             api.instance(name + ".instance", name);
         } else if (p.peekNextToken("sphere")) {
             UI.printInfo("[API] Reading sphere ...");
             String name = api.getUniqueName("sphere");
             api.geometry(name, new Sphere());
             if (transform != null)
                 api.parameter("transform", transform);
             else if (p.peekNextToken("c")) {
                 float x = p.getNextFloat();
                 float y = p.getNextFloat();
                 float z = p.getNextFloat();
                 p.checkNextToken("r");
                 float radius = p.getNextFloat();
                 api.parameter("transform", Matrix4.translation(x, y, z).multiply(Matrix4.scale(radius)));
             } else
                 api.parameter("transform", parseMatrix());
             api.parameter("shaders", shaders);
             api.instance(name + ".instance", name);
         } else if (p.peekNextToken("banchoff")) {
             UI.printInfo("[API] Reading banchoff ...");
             String name = api.getUniqueName("banchoff");
             api.geometry(name, new BanchoffSurface());
             api.parameter("transform", transform);
             api.parameter("shaders", shaders);
             api.instance(name + ".instance", name);
         } else if (p.peekNextToken("torus")) {
             UI.printInfo("[API] Reading torus ...");
             p.checkNextToken("r");
             api.parameter("radiusInner", p.getNextFloat());
             api.parameter("radiusOuter", p.getNextFloat());
             String name = api.getUniqueName("torus");
             api.geometry(name, new Torus());
             api.parameter("transform", transform);
             api.parameter("shaders", shaders);
             api.instance(name + ".instance", name);
         } else if (p.peekNextToken("plane")) {
             UI.printInfo("[API] Reading plane ...");
             p.checkNextToken("p");
             api.parameter("center", parsePoint());
             if (p.peekNextToken("n")) {
                 api.parameter("normal", parseVector());
             } else {
                 p.checkNextToken("p");
                 api.parameter("point1", parsePoint());
                 p.checkNextToken("p");
                 api.parameter("point2", parsePoint());
             }
             String name = api.getUniqueName("plane");
             api.geometry(name, new Plane());
             api.parameter("shaders", shaders);
             if (transform != null)
                 api.parameter("transform", transform);
             api.instance(name + ".instance", name);
         } else if (p.peekNextToken("cornellbox")) {
             UI.printInfo("[API] Reading cornell box ...");
             if (transform != null)
                 UI.printWarning("[API] Instancing is not supported on cornell box -- ignoring transform");
             p.checkNextToken("corner0");
             api.parameter("corner0", parsePoint());
             p.checkNextToken("corner1");
             api.parameter("corner1", parsePoint());
             p.checkNextToken("left");
             api.parameter("leftColor", parseColor());
             p.checkNextToken("right");
             api.parameter("rightColor", parseColor());
             p.checkNextToken("top");
             api.parameter("topColor", parseColor());
             p.checkNextToken("bottom");
             api.parameter("bottomColor", parseColor());
             p.checkNextToken("back");
             api.parameter("backColor", parseColor());
             p.checkNextToken("emit");
             api.parameter("radiance", parseColor());
             if (p.peekNextToken("samples"))
                 api.parameter("samples", p.getNextInt());
             CornellBox box = new CornellBox();
             box.init(api.getUniqueName("cornellbox"), api);
         } else if (p.peekNextToken("generic-mesh")) {
             p.checkNextToken("name");
             String name = p.getNextToken();
             UI.printInfo("[API] Reading generic mesh: %s ... ", name);
             // parse vertices
             p.checkNextToken("points");
             int np = p.getNextInt();
             api.parameter("points", "point", "vertex", parseFloatArray(np * 3));
             // parse triangle indices
             p.checkNextToken("triangles");
             int nt = p.getNextInt();
             api.parameter("triangles", parseIntArray(nt * 3));
             // parse normals
             p.checkNextToken("normals");
             if (p.peekNextToken("vertex"))
                 api.parameter("normals", "vector", "vertex", parseFloatArray(np * 3));
             else if (p.peekNextToken("facevarying"))
                 api.parameter("normals", "vector", "facevarying", parseFloatArray(nt * 9));
             else
                 p.checkNextToken("none");
             // parse texture coordinates
             p.checkNextToken("uvs");
             if (p.peekNextToken("vertex"))
                 api.parameter("uvs", "texcoord", "vertex", parseFloatArray(np * 2));
             else if (p.peekNextToken("facevarying"))
                 api.parameter("uvs", "texcoord", "facevarying", parseFloatArray(nt * 6));
             else
                 p.checkNextToken("none");
             if (multiShader) {
                 p.checkNextToken("face_shaders");
                 api.parameter("faceshaders", parseIntArray(nt));
             }
             api.geometry(name, new Mesh());
             api.parameter("shaders", shaders);
             if (transform != null)
                 api.parameter("transform", transform);
             api.instance(name + ".instance", name);
         } else if (p.peekNextToken("hair")) {
             p.checkNextToken("name");
             String name = p.getNextToken();
             UI.printInfo("[API] Reading hair curves: %s ... ", name);
             p.checkNextToken("segments");
             api.parameter("segments", p.getNextInt());
             p.checkNextToken("width");
             api.parameter("width", p.getNextFloat());
             p.checkNextToken("points");
             api.parameter("points", "point", "vertex", parseFloatArray(p.getNextInt()));
             api.geometry(name, new Hair());
             api.parameter("shaders", shaders);
             if (transform != null)
                 api.parameter("transform", transform);
             api.instance(name + ".instance", name);
         } else if (p.peekNextToken("janino-tesselatable")) {
             p.checkNextToken("name");
             String name = p.getNextToken();
             UI.printInfo("[API] Reading procedural primitive: %s ... ", name);
             String code = p.getNextCodeBlock();
             try {
                 Tesselatable tess = (Tesselatable) ClassBodyEvaluator.createFastClassBodyEvaluator(new Scanner(null, new StringReader(code)), Tesselatable.class, ClassLoader.getSystemClassLoader());
                 api.geometry(name, tess);
             } catch (CompileException e) {
                 UI.printDetailed("[API] Compiling: %s", code);
                 UI.printError("[API] %s", e.getMessage());
                 e.printStackTrace();
                 name = null;
             } catch (ParseException e) {
                 UI.printDetailed("[API] Compiling: %s", code);
                 UI.printError("[API] %s", e.getMessage());
                 e.printStackTrace();
                 name = null;
             } catch (ScanException e) {
                 UI.printDetailed("[API] Compiling: %s", code);
                 UI.printError("[API] %s", e.getMessage());
                 e.printStackTrace();
                 name = null;
             } catch (IOException e) {
                 UI.printDetailed("[API] Compiling: %s", code);
                 UI.printError("[API] %s", e.getMessage());
                 e.printStackTrace();
                 name = null;
             }
             if (name != null) {
                 api.parameter("shaders", shaders);
                 if (transform != null)
                     api.parameter("transform", transform);
                 api.instance(name + ".instance", name);
             }
         } else if (p.peekNextToken("teapot")) {
            String name = api.getUniqueName("teapot");
             UI.printInfo("[API] Reading teapot: %s ... ", name);
             if (p.peekNextToken("subdivs"))
                 api.parameter("subdivs", p.getNextInt());
             if (p.peekNextToken("smooth"))
                 api.parameter("smooth", p.getNextBoolean());
             api.geometry(name, new Teapot());
             api.parameter("shaders", shaders);
             if (transform != null)
                 api.parameter("transform", transform);
             api.instance(name + ".instance", name);
         } else if (p.peekNextToken("gumbo")) {
            String name = api.getUniqueName("gumbo");
            UI.printInfo("[API] Reading teapot: %s ... ", name);
             if (p.peekNextToken("subdivs"))
                 api.parameter("subdivs", p.getNextInt());
             if (p.peekNextToken("smooth"))
                 api.parameter("smooth", p.getNextBoolean());
             api.geometry(name, new Gumbo());
             api.parameter("shaders", shaders);
             if (transform != null)
                 api.parameter("transform", transform);
             api.instance(name + ".instance", name);
         } else
             UI.printWarning("[API] Unrecognized object type: %s", p.getNextToken());
         p.checkNextToken("}");
     }
 
     private void parseInstanceBlock(SunflowAPI api) throws ParserException, IOException {
         p.checkNextToken("{");
         p.checkNextToken("name");
         String name = p.getNextToken();
         UI.printInfo("Reading instance: %s ...", name);
         p.checkNextToken("geometry");
         String geoname = p.getNextToken();
         p.checkNextToken("transform");
         api.parameter("transform", parseMatrix());
         String[] shaders;
         if (p.peekNextToken("shaders")) {
             int n = p.getNextInt();
             shaders = new String[n];
             for (int i = 0; i < n; i++)
                 shaders[i] = p.getNextToken();
         } else {
             p.checkNextToken("shader");
             shaders = new String[] { p.getNextToken() };
         }
         api.parameter("shaders", shaders);
         api.instance(name, geoname);
         p.checkNextToken("}");
     }
 
     private void parseLightBlock(SunflowAPI api) throws ParserException, IOException {
         p.checkNextToken("{");
         p.checkNextToken("type");
         if (p.peekNextToken("mesh")) {
             UI.printWarning("[API] Deprecated light type: mesh");
             p.checkNextToken("name");
             String name = p.getNextToken();
             UI.printInfo("[API] Reading light mesh: %s ...", name);
             p.checkNextToken("emit");
             api.parameter("radiance", parseColor());
             int samples = numLightSamples;
             if (p.peekNextToken("samples"))
                 samples = p.getNextInt();
             else
                 UI.printWarning("[API] Samples keyword not found - defaulting to %d", samples);
             api.parameter("samples", samples);
             int numVertices = p.getNextInt();
             int numTriangles = p.getNextInt();
             float[] points = new float[3 * numVertices];
             int[] triangles = new int[3 * numTriangles];
             for (int i = 0; i < numVertices; i++) {
                 p.checkNextToken("v");
                 points[3 * i + 0] = p.getNextFloat();
                 points[3 * i + 1] = p.getNextFloat();
                 points[3 * i + 2] = p.getNextFloat();
                 // ignored
                 p.getNextFloat();
                 p.getNextFloat();
                 p.getNextFloat();
                 p.getNextFloat();
                 p.getNextFloat();
             }
             for (int i = 0; i < numTriangles; i++) {
                 p.checkNextToken("t");
                 triangles[3 * i + 0] = p.getNextInt();
                 triangles[3 * i + 1] = p.getNextInt();
                 triangles[3 * i + 2] = p.getNextInt();
             }
             api.parameter("points", "point", "vertex", points);
             api.parameter("triangles", triangles);
             MeshLight mesh = new MeshLight();
             mesh.init(name, api);
         } else if (p.peekNextToken("point")) {
             UI.printInfo("[API] Reading point light ...");
             Color pow;
             if (p.peekNextToken("color")) {
                 pow = parseColor();
                 p.checkNextToken("power");
                 float po = p.getNextFloat();
                 pow.mul(po);
             } else {
                 UI.printWarning("[API] Deprecated color specification - please use color and power instead");
                 p.checkNextToken("power");
                 pow = parseColor();
             }
             p.checkNextToken("p");
             float px = p.getNextFloat();
             float py = p.getNextFloat();
             float pz = p.getNextFloat();
             api.pointLight(api.getUniqueName("pointLight"), px, py, pz, pow);
         } else if (p.peekNextToken("directional")) {
             UI.printInfo("[API] Reading directional light ...");
             p.checkNextToken("source");
             Point3 s = parsePoint();
             api.parameter("source", s);
             p.checkNextToken("target");
             Point3 t = parsePoint();
             api.parameter("dir", Point3.sub(t, s, new Vector3()));
             p.checkNextToken("radius");
             api.parameter("radius", p.getNextFloat());
             p.checkNextToken("emit");
             Color e = parseColor();
             if (p.peekNextToken("intensity")) {
                 float i = p.getNextFloat();
                 e.mul(i);
             } else
                 UI.printWarning("[API] Deprecated color specification - please use emit and intensity instead");
             api.parameter("radiance", e);
             api.light(api.getUniqueName("dirlight"), new DirectionalSpotlight());
         } else if (p.peekNextToken("ibl")) {
             UI.printInfo("[API] Reading image based light ...");
             p.checkNextToken("image");
             api.parameter("texture", p.getNextToken());
             p.checkNextToken("center");
             api.parameter("center", parseVector());
             p.checkNextToken("up");
             api.parameter("up", parseVector());
             p.checkNextToken("lock");
             api.parameter("fixed", p.getNextBoolean());
             int samples = numLightSamples;
             if (p.peekNextToken("samples"))
                 samples = p.getNextInt();
             else
                 UI.printWarning("[API] Samples keyword not found - defaulting to %d", samples);
             api.parameter("samples", samples);
             ImageBasedLight ibl = new ImageBasedLight();
             ibl.init(api.getUniqueName("ibl"), api);
         } else if (p.peekNextToken("meshlight")) {
             p.checkNextToken("name");
             String name = p.getNextToken();
             UI.printInfo("[API] Reading meshlight: %s ...", name);
             p.checkNextToken("emit");
             Color e = parseColor();
             if (p.peekNextToken("radiance")) {
                 float r = p.getNextFloat();
                 e.mul(r);
             } else
                 UI.printWarning("[API] Deprecated color specification - please use emit and radiance instead");
             api.parameter("radiance", e);
             int samples = numLightSamples;
             if (p.peekNextToken("samples"))
                 samples = p.getNextInt();
             else
                 UI.printWarning("[API] Samples keyword not found - defaulting to %d", samples);
             api.parameter("samples", samples);
             // parse vertices
             p.checkNextToken("points");
             int np = p.getNextInt();
             api.parameter("points", "point", "vertex", parseFloatArray(np * 3));
             // parse triangle indices
             p.checkNextToken("triangles");
             int nt = p.getNextInt();
             api.parameter("triangles", parseIntArray(nt * 3));
             MeshLight mesh = new MeshLight();
             mesh.init(name, api);
         } else
             UI.printWarning("[API] Unrecognized object type: %s", p.getNextToken());
         p.checkNextToken("}");
     }
 
     private Color parseColor() throws IOException, ParserException {
         if (p.peekNextToken("{")) {
             String space = p.getNextToken();
             Color c = null;
             if (space.equals("sRGB nonlinear")) {
                 float r = p.getNextFloat();
                 float g = p.getNextFloat();
                 float b = p.getNextFloat();
                 c = new Color(r, g, b);
                 c.toLinear();
             } else if (space.equals("sRGB linear")) {
                 float r = p.getNextFloat();
                 float g = p.getNextFloat();
                 float b = p.getNextFloat();
                 c = new Color(r, g, b);
             } else
                 UI.printWarning("[API] Unrecognized color space: %s", space);
             p.checkNextToken("}");
             return c;
         } else {
             float r = p.getNextFloat();
             float g = p.getNextFloat();
             float b = p.getNextFloat();
             return new Color(r, g, b);
         }
     }
 
     private Point3 parsePoint() throws IOException {
         float x = p.getNextFloat();
         float y = p.getNextFloat();
         float z = p.getNextFloat();
         return new Point3(x, y, z);
     }
 
     private Vector3 parseVector() throws IOException {
         float x = p.getNextFloat();
         float y = p.getNextFloat();
         float z = p.getNextFloat();
         return new Vector3(x, y, z);
     }
 
     private int[] parseIntArray(int size) throws IOException {
         int[] data = new int[size];
         for (int i = 0; i < size; i++)
             data[i] = p.getNextInt();
         return data;
     }
 
     private float[] parseFloatArray(int size) throws IOException {
         float[] data = new float[size];
         for (int i = 0; i < size; i++)
             data[i] = p.getNextFloat();
         return data;
     }
 
     private Matrix4 parseMatrix() throws IOException, ParserException {
         if (p.peekNextToken("row")) {
             return new Matrix4(parseFloatArray(16), true);
         } else if (p.peekNextToken("col")) {
             return new Matrix4(parseFloatArray(16), false);
         } else {
             Matrix4 m = Matrix4.IDENTITY;
             p.checkNextToken("{");
             while (!p.peekNextToken("}")) {
                 Matrix4 t = null;
                 if (p.peekNextToken("translate")) {
                     float x = p.getNextFloat();
                     float y = p.getNextFloat();
                     float z = p.getNextFloat();
                     t = Matrix4.translation(x, y, z);
                 } else if (p.peekNextToken("scaleu")) {
                     float s = p.getNextFloat();
                     t = Matrix4.scale(s);
                 } else if (p.peekNextToken("scale")) {
                     float x = p.getNextFloat();
                     float y = p.getNextFloat();
                     float z = p.getNextFloat();
                     t = Matrix4.scale(x, y, z);
                 } else if (p.peekNextToken("rotatex")) {
                     float angle = p.getNextFloat();
                     t = Matrix4.rotateX((float) Math.toRadians(angle));
                 } else if (p.peekNextToken("rotatey")) {
                     float angle = p.getNextFloat();
                     t = Matrix4.rotateY((float) Math.toRadians(angle));
                 } else if (p.peekNextToken("rotatez")) {
                     float angle = p.getNextFloat();
                     t = Matrix4.rotateZ((float) Math.toRadians(angle));
                 } else if (p.peekNextToken("rotate")) {
                     float x = p.getNextFloat();
                     float y = p.getNextFloat();
                     float z = p.getNextFloat();
                     float angle = p.getNextFloat();
                     t = Matrix4.rotate(x, y, z, (float) Math.toRadians(angle));
                 } else
                     UI.printWarning("[API] Unrecognized transformation type: %s", p.getNextToken());
                 if (t != null)
                     m = t.multiply(m);
             }
             return m;
         }
     }
 }
