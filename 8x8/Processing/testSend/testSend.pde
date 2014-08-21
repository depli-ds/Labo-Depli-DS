import processing.serial.*;

Serial myPort;
int feedback;
int button = 0;

void setup(){
  size (100, 200);
  println(Serial.list());
  String portName = Serial.list()[5];
  myPort = new Serial(this, portName, 9600);
}

void draw(){
  for(int i = 0; i < 10; i++){
    myPort.write(i);
    println(i, " sent");
    delay(100);
  }
}

