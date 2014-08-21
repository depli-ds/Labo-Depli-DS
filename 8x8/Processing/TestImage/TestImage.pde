import processing.serial.*;

Serial myPort;
PImage myImage; 
int loc = 0;
int xWidth = 0;
int yHeight = 0;

void setup(){
  size(8, 8);
  myImage = loadImage("FLV.png");
  println(Serial.list());
  String portName = Serial.list()[2];
  myPort = new Serial(this, portName, 9600);
}

void draw(){
  image(myImage, 0, 0); 

  for(int x = 0; x < myImage.width; x++){
    for(int y = 0; y < myImage.height; y ++){
      loc = y + x*height;
      xWidth = x;
      yHeight = y;
      String red = hex((int)red(myImage.get(x,y)));
      String green = hex((int)green(myImage.get(x,y)));
      String blue = hex((int)blue(myImage.get(x,y)));
      //println("A la position ", loc, " R = ", red, ", G : ", green, ", B : ", blue);
      
      myPort.write(xWidth);
      println("x: ", xWidth, "sent");
      myPort.write(yHeight);
      println("y : ", yHeight, "sent");
      myPort.write(red);
      println("color : ", red, "sent");
      delay(2000);
      
      if (loc == 63){
        println("--- FINISH ---");
      }
    }
  }
}

