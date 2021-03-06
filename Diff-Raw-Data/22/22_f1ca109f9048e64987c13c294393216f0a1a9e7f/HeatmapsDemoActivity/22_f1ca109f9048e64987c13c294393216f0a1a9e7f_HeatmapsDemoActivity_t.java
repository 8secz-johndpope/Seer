 package com.google.maps.android.utils.demo;
 
 import android.graphics.Bitmap;
 import android.graphics.Canvas;
 import android.graphics.Paint;
 import android.util.Log;
 
 import com.google.android.gms.maps.CameraUpdateFactory;
 import com.google.android.gms.maps.model.BitmapDescriptor;
 import com.google.android.gms.maps.model.BitmapDescriptorFactory;
 import com.google.android.gms.maps.model.GroundOverlayOptions;
 import com.google.android.gms.maps.model.LatLng;
 import com.google.android.gms.maps.model.LatLngBounds;
 import com.google.maps.android.heatmaps.HeatmapConstants;
 import com.google.maps.android.heatmaps.HeatmapUtil;
 
 import java.util.Arrays;
 
 public class HeatmapsDemoActivity extends BaseDemoActivity {
 
     /** where sydney is */
     private final LatLng SYDNEY = new LatLng(-33.865955, 151.195891);
 
     @Override
     protected void startDemo() {
 
 
         getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(SYDNEY, 16));
 
         double[] kernel = HeatmapUtil.generateKernel(5, 1.5);
         Log.e("kernel", Arrays.toString(kernel));
 
         // test with radius 2
         double[][] grid = new double[5][5];
         grid[2][2] = 2;
         grid[2][1] = 1;
         grid[1][2] = 1;
         grid[2][3] = 1;
         grid[3][2] = 1;
         double[] testKernel = {0.5, 1, 0.5};
         double[][] convolved = HeatmapUtil.convolve(grid, testKernel);
         printGrid(convolved);
 
         int[] colorMapTest = HeatmapUtil.generateColorMap(HeatmapConstants.DEFAULT_HEATMAP_GRADIENT, 102, 1);
         Log.e("map", Arrays.toString(colorMapTest));
 
         //Bitmap colorMap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
 
         /*
         int colours[] = new int[256*256];
 
         int i, j, count = 0;
         for (i = 0; i < 256; i++) {
             for (j = 0; j < 256; j++) {
                 if(j < colorMapTest.length) colours[count] = colorMapTest[j];
                 else colours[count] = colorMapTest[colorMapTest.length - 1];
                 count++;
             }
         }
         // public void drawBitmap (int[] colors, int offset, int stride, float x, float y, int width, int height, boolean hasAlpha, Paint paint)
         // set paint to null
         colorMap.setPixels(colours,0, 256,0, 0, 256, 256); */
 
         int dim = 256;
         int radius = 5;
         double[][] bigGrid = new double[dim + 2*radius][dim + 2*radius];
         bigGrid[100][100] = 10;
         bigGrid[200][200] = 10;
        bigGrid[200][100] = 10;
        bigGrid[240][240] = 10;
         double[] bigKernel = HeatmapUtil.generateKernel(radius, radius/3.0);
         double[][] bigConvolved = HeatmapUtil.convolve(bigGrid, bigKernel);
         //printGrid(bigConvolved);
         Bitmap colorMap = HeatmapUtil.colorize(bigConvolved, colorMapTest, 10);
 
         BitmapDescriptor image = BitmapDescriptorFactory.fromBitmap(colorMap);
         LatLng northeast = new LatLng(-33.865, 151.196);
         LatLng southwest = new LatLng(-33.866, 151.195);
         LatLngBounds bounds = new LatLngBounds(southwest, northeast);
         getMap().addGroundOverlay (new GroundOverlayOptions()
                 .image(image)
                 .positionFromBounds(bounds));
 
 
         Bitmap square = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
         Canvas canvas = new Canvas(square);
         // left top right bottom
         Paint paint = new Paint();
         canvas.drawRect(0,0,1,256, paint);
         canvas.drawRect(0,0,256,1, paint);
 
         BitmapDescriptor bSquare = BitmapDescriptorFactory.fromBitmap(square);
         getMap().addGroundOverlay (new GroundOverlayOptions()
                 .image(bSquare)
                 .positionFromBounds(bounds));
 
     }
 
 
 
 
     /**
      * Helper function for testing - print grid to Log.e
      * @param grid Grid to print
      */
     public static void printGrid(double[][] grid) {
         int i;
         for (i = 0; i < grid.length; i ++) {
             Log.e("grid"+i, Arrays.toString(grid[i]));
         }
     }
 }
