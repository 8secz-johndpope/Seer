 /*
  ** 2014 July 10
  **
  ** The author disclaims copyright to this source code.  In place of
  ** a legal notice, here is a blessing:
  **    May you do good and not evil.
  **    May you find forgiveness for yourself and forgive others.
  **    May you share freely, never taking more than you give.
  */
 package info.ata4.unity.engine;
 
 import info.ata4.unity.serdes.UnityObject;
 
// SubMesh (Unity 4)
 //   unsigned int firstByte
 //   unsigned int indexCount
 //   int topology
 //   unsigned int firstVertex
 //   unsigned int vertexCount
 //   AABB localAABB

// SubMesh (Unity 3)
//   UInt32 firstByte
//   UInt32 indexCount
//   UInt32 isTriStrip
//   UInt32 triangleCount
//   UInt32 firstVertex
//   UInt32 vertexCount
//   AABB localAABB

 public class SubMesh {
     
     public final Long firstByte;
     public final Long indexCount;
    public final Long topology;
     public final Long firstVertex;
     public final Long vertexCount;
 
     SubMesh(UnityObject obj) {
         firstByte = obj.getValue("firstByte");
         indexCount = obj.getValue("indexCount");
        if (obj.hasValue("topology")) {
            topology = obj.getValue("topology");
        } else {
            topology = obj.getValue("isTriStrip");
        }
         firstVertex = obj.getValue("firstVertex");
         vertexCount = obj.getValue("vertexCount");
     }
     
 }
