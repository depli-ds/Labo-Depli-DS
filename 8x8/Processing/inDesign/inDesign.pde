PImage myImage;
int loc = 0;
int xWidth = 0;
int yHeight = 0;
int red;
int green;
int blue;
int grey;
int[] data;

PrintWriter output_data;

void setup (){
  size(8, 8);
  myImage = loadImage("FLV.png");
  output_data = createWriter("data.txt");
//  String[] stuff = loadStrings("data.txt");
//  data = int(split(stuff[0],'\n'));
}

void draw(){
  image(myImage, 0, 0);
  xWidth = myImage.width;
  yHeight = myImage.height;
  output_data.println(xWidth);
  output_data.println(yHeight);
  for(int x = 0; x < myImage.width; x++){
    for(int y = 0; y < myImage.height; y++){
      loc = y + x*height;
      red = int(red(myImage.get(x,y)));
      green = int(green(myImage.get(x,y)));
      blue = int(blue(myImage.get(x,y)));
      grey = (red + green + blue)/3;
      //println("A la position ", loc, " R = ", red, ", G : ", green, ", B : ", blue);
      println("A la position", loc, ", G = ", grey);
      output_data.println(grey);
    }
  }
  output_data.flush();
  output_data.close();
  exit();
}
