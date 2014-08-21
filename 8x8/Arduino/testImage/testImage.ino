#include <Rainbowduino.h>

int xWidth;
int yHeight;
int color;

void setup(){
  Serial.begin(9600);
  Rb.init();
}

void loop(){
  if (Serial.available() >= 3){
    xWidth = Serial.read();
    yHeight = Serial.read();
    color = Serial.read();
    //Serial.println (xWidth);
    //Serial.println(yHeight);
    Serial.println(color);
    Rb.setPixelXY(xWidth, yHeight, color, color, color);   
  } else {
    Serial.println("Nothing is detected");
    for (int i = 0; i < 8; i++){
      for(int j = 0; j < 8; j++){
        Rb.setPixelXY(i, j, 0, 0, 0);
      }
    }
  }

   delay(2000);
}
