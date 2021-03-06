 package thejavapowder;
 
 public class Methods {
 
     Variables var = new Variables();
     char t;
 
     @SuppressWarnings("static-access")
     public void createParticle(int x, int y, byte id) {
         for (int i = 0; i < var.reaction[7]; i++) {
             if (var.Map[x][y - 1] == -127) {
                 var.Map[x][y - 1] = id;
             } else if (var.Map[x][y + 1] == -127) {
                 var.Map[x][y + 1] = id;
             } else if (var.Map[x + 1][y] == -127) {
                 var.Map[x + 1][y] = id;
             } else if (var.Map[x - 1][y] == -127) {
                 var.Map[x - 1][y] = id;
             } else if (var.Map[x + 1][y + 1] == -127) {
                 var.Map[x + 1][y + 1] = id;
             } else if (var.Map[x - 1][y + 1] == -127) {
                 var.Map[x - 1][y + 1] = id;
             } else if (var.Map[x + 1][y - 1] == -127) {
                 var.Map[x + 1][y - 1] = id;
             } else if (var.Map[x - 1][y - 1] == -127) {
                 var.Map[x - 1][y - 1] = id;
             }
         }
     }
 
     @SuppressWarnings("static-access")
     public void getReactives(byte id) {
        if(id != -127)
        {
         this.var.reactives = var.Elements[id].reactives;
        }
        else
        {
          this.var.reactives = null;
        }
         return;
     }
 
 
     @SuppressWarnings("static-access")
     public int getWeight(byte ID) {
         if(ID >= 0 && ID < var.NUM_ELS)
         {
             return var.Elements[ID].weight;
         }
         else
         {
             return -127;
         }
     }
 
     @SuppressWarnings("static-access")
     public int getDTemp(byte ID) {
         if(ID >= 0 && ID < var.NUM_ELS)
         {
             return var.Elements[ID].defaultTemp;
         }
         else
         {
             return -127;
         }
     }
 
 
     @SuppressWarnings("static-access")
     public void getReaction(byte id, byte reactId) {
         var.reaction = var.Elements[id].react[reactId];
         return;
     }
 
     @SuppressWarnings("static-access")
     public void getSurroundings(int x, int y) {
         if (x < var.Width - 1 && x > 0 && y < var.Height - 1 && y > 0) {
             var.surArray[0] = var.Map[x - 1][y - 1];
             var.surArray[1] = var.Map[x][y - 1];
             var.surArray[2] = var.Map[x + 1][y - 1];
             var.surArray[3] = var.Map[x - 1][y];
             // Note the abscence of the center square
             var.surArray[4] = var.Map[x + 1][y];
             var.surArray[5] = var.Map[x - 1][y + 1];
             var.surArray[6] = var.Map[x][y + 1];
             var.surArray[7] = var.Map[x + 1][y + 1];
         }
 
     }
 
     @SuppressWarnings("static-access")
     public char getType(int x, int y) {
         if(var.Map[x][y] >= 0 && var.Map[x][y] < var.NUM_ELS)
         {
             t = var.Elements[var.Map[x][y]].state;
         }
         else
         {
             t = 's';
         }
         return t;
 
     }
 
     @SuppressWarnings("static-access")
     public boolean GetConductive(byte id) {
         if (id >= 0 && id < var.NUM_ELS)
         {
             var.conductive = var.Elements[id].conductive;
             return var.conductive;
         }
         var.conductive = false;
         return false;
     }
 
 }
 
