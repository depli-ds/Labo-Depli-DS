import processing.core.*; 
import processing.xml.*; 

import themidibus.*; 
import com.rngtng.launchpad.*; 
import processing.serial.*; 
import com.rngtng.rainbowduino.*; 

import java.applet.*; 
import java.awt.*; 
import java.awt.image.*; 
import java.awt.event.*; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class mtXcontrol extends PApplet {

/*
 * mtXcontrol - a LED Matrix Editor - version 1.1
 */
PFont fontA;
PFont fontLetter;

Matrix matrix;
Device device;

int border = 10;

int offY = 30;
int offX = 30;

int current_delay = 0;
int current_speed = 10;

boolean record  = true;
boolean color_mode  = false;

boolean keyCtrl = false;
boolean keyMac  = false;
boolean keyAlt  = false;

boolean update = true;
Button[] buttons;

int hide_button_index;

/* +++++++++++++++++++++++++++++ */

public void setup() {
  frame.setIconImage( getToolkit().getImage("sketch.ico") );

  matrix = new Matrix(8, 8);
  device = new LaunchpadDevice(this); 
  if(device == null || !device.enabled()) device = new RainbowduinoDevice(this);

  device.setColorScheme();

  size(780,720);
  smooth();
  noStroke();
  fontA = loadFont("Courier-Bold-32.vlw");
  fontLetter = loadFont("ArialMT-20.vlw");
  setup_buttons();
  frameRate(15);
}

public void setup_buttons() {
  buttons = new Button[60]; // width + height + ???
  int offset = 10;
  int button_index = 0;
  int y_pos = 0;

int button_color = 0xff333333;
int button_color_over = 0xff999999;
  int button_size = 15;

  for(int i = 0; i < matrix.rows; i++ ) {
    int x = offX + matrix.width() + offset;
    int y = offY + i * matrix.rad + matrix.border / 2;
    buttons[button_index++] = new RectButton( x, y, button_size, matrix.rad - matrix.border, button_color, button_color_over);
  }
  for(int i = 0; i < matrix.cols; i++ ) {
    int x = offX + i * matrix.rad + matrix.border / 2;
    int y = offY + matrix.width() + offset;
    buttons[button_index++] = new RectButton( x, y, matrix.rad - matrix.border, button_size, button_color, button_color_over);
  }
  buttons[button_index++] = new SquareButton( offX + matrix.width() + offset, offY + matrix.width() + offset, button_size, button_color, button_color_over );

  int button_x = offX + matrix.width() + offset + 30;
  buttons[button_index++] = new ActionToggleButton( "Mode: RECORD",  "Mode: PLAY",    "10",   button_x, y_pos += 30);
  buttons[button_index++] = new ActionToggleButton( "Device: SLAVE",  "Device: FREE", "a+10", button_x, y_pos += 30);
  if(! (device instanceof StandaloneDevice && device.enabled()) ) buttons[button_index-1].disable();

  buttons[button_index++] = new FrameChooser(offX, offY + matrix.height() + 40, 59, 10);

  hide_button_index = button_index;
  buttons[button_index++] = new TextElement( "Load from:", button_x, y_pos += 30);
  buttons[button_index++] = new ActionButton( "File",    "m+L", button_x,      y_pos += 30, 65, 25);
  buttons[button_index++] = new ActionButton( "Device",  "a+L", button_x + 67, y_pos,       65, 25);
  if(! (device instanceof StandaloneDevice && device.enabled()) ) buttons[button_index-1].disable();

  buttons[button_index++] = new TextElement( "Save to:", button_x, y_pos += 30);
  buttons[button_index++] = new ActionButton( "File",    "m+S", button_x,      y_pos += 30, 65, 25);
  buttons[button_index++] = new ActionButton( "Device",  "a+S", button_x + 67, y_pos,       65, 25);
  if(! (device instanceof StandaloneDevice && device.enabled()) ) buttons[button_index-1].disable();

  buttons[button_index++] = new TextElement( "Color:", button_x, y_pos += 40);
  y_pos += 30;

  PixelColor pc = new PixelColor(); 
  int off = 140 / pc.numColors();
  for(int r = 0; r < PixelColorScheme.R.length; r++) {
    for(int g = 0; g < PixelColorScheme.G.length; g++) {
      for(int b = 0; b < PixelColorScheme.B.length; b++) {   
        buttons[button_index++] = new MiniColorButton( button_x + pc.to_int() * off, y_pos, off, 20, pc.clone() );
        pc.next_color();
      }
    }
  }

  buttons[button_index++] = new TextElement( "Frame:", button_x, y_pos += 20);  
  buttons[button_index++] = new ActionButton( "Add",    " ", button_x, y_pos += 30);
  buttons[button_index++] = new ActionButton( "Delete", "D", button_x, y_pos += 30);

  buttons[button_index++] = new ActionButton( "^", "c+38", button_x + 47,  y_pos += 50, 40, 25);
  buttons[button_index++] = new ActionButton( "<", "c+37", button_x,       y_pos += 20, 40, 25);
  buttons[button_index++] = new ActionButton( ">", "c+39", button_x + 94,  y_pos,       40, 25);
  buttons[button_index++] = new ActionButton( "v", "c+40", button_x + 47,  y_pos += 15, 40, 25);

  buttons[button_index++] = new ActionButton( "Paste",  "m+V", button_x, y_pos += 50);
  buttons[button_index++] = new ActionButton( "Copy",   "m+C", button_x, y_pos += 30);
  buttons[button_index++] = new ActionButton( "Fill",   "F",   button_x, y_pos += 30);
  buttons[button_index++] = new ActionButton( "Clear",  "X",   button_x, y_pos += 30);
}

/* ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ */

public void draw()
{  
  if(update) {
    background(45);
    fill(50);
    rect(offX - matrix.border / 2, offY - matrix.border / 2, matrix.width() + matrix.border, matrix.height() + matrix.border);
    image(matrix.current_frame_image(), offX, offY);
    
    for(int i = 0; i < buttons.length; i++ ) {
      if(buttons[i] == null) break;
      buttons[i].display();
    }

    fill(255); //white
    if(!record) {
      text("Speed: " + current_speed, offX + matrix.width() + 65, 110);
    }

    if(!device.enabled()) {
      text("No output device found, running in standalone mode", 120, 20);  
    }

    device.write_frame(matrix.current_frame());
    update = false;
  }
  if(!record) next_frame();
}


/* +++++++++++++++++++++++++++++ */

public void next_frame() {
  if(current_delay < 1) {
    current_delay = current_speed;
    matrix.next_frame();
    mark_for_update();
  }
  current_delay--;
}

/* +++++++++++++++ ACTIONS +++++++++++++++ */
public void mouseDragged() {
  if(!record) return;
  if(matrix.click(mouseX - offX, mouseY - offY, true)) mark_for_update();
}

public void mousePressed() {
  if(!record) return;
  if(matrix.click(mouseX - offX, mouseY - offY, false)) mark_for_update();
}

public void mouseMoved() {
  for(int i = 0; i < buttons.length; i++ ) {
    if(buttons[i] == null) break;
    if(buttons[i].over()) mark_for_update();
  }
}

public void mouseClicked() {
  for(int i = 0; i < buttons.length; i++ ) {
    if(buttons[i] == null) break;
    if(buttons[i].clicked()) mark_for_update();
  }
}


public void keyPressed() {
  if(keyCode == 17)  keyCtrl = true; //control
  if(keyCode == 18)  keyAlt  = true; //alt
  if(keyCode == 157) keyMac  = true; //mac
  if(keyCode == 67) color_mode = true; //C
  
  //println("pressed " + key + " " + keyCode + " " +keyMac+ " "+  keyCtrl + " "+ keyAlt );

  if(color_mode) {
     if(keyCode == 37) matrix.current_color.previous_color(); //arrow left
     if(keyCode == 39) matrix.current_color.next_color();  //arrow right   
     mark_for_update();
     return;
  }

  for(int i = 0; i < buttons.length; i++ ) {
    if(buttons[i] == null) break;
    if(buttons[i].key_pressed(keyCode, keyMac, keyCtrl, keyAlt)) {
      mark_for_update();  
      return;
    }
  }
   
  if(keyAlt) {
    if(device instanceof StandaloneDevice) {
      if(keyCtrl) {
        if(keyCode == 37) ((StandaloneDevice) device).brightnessDown();   //arrow left
        if(keyCode == 39) ((StandaloneDevice) device).brightnessUp(); //arrow right        
      }
      else {        
        if(keyCode == 37) ((StandaloneDevice) device).speedUp();   //arrow left
        if(keyCode == 39) ((StandaloneDevice) device).speedDown(); //arrow right        
      }
    }
  }
  else if(keyCtrl) {
    PixelColor pc = null;
    if(keyCode >= 48) pc = matrix.current_frame().set_letter(PApplet.parseChar(keyCode), fontLetter, matrix.current_color);
    if( pc != null )  {
      matrix.current_color = pc;
      mark_for_update(); 
    }
    return;
  }
  else {
    if(!record) {
      if( keyCode == 37) speed_up();     //arrow left
      if( keyCode == 39) speed_down();   //arrow right
    }
  }
}

public void keyReleased() {
  if( keyCode == 17 )  keyCtrl = false;
  if( keyCode == 18 )  keyAlt  = false;
  if( keyCode == 157 ) keyMac  = false;
  if( keyCode == 67 ) color_mode = false;
}

public void mark_for_update() {
  update = true;
}

/* +++++++++++++++ modes +++++++++++++++ */
public void toggle_mode() {
  matrix.current_frame_nr = 0;
  record = !record;
  for(int i = hide_button_index; i < buttons.length; i++ ) {
    if(buttons[i] == null) break;
    if(record) buttons[i].toggle(); 
    else buttons[i].hide();
  }
  if(record) buttons[hide_button_index-1].enable(); 
  else buttons[hide_button_index-1].disable();
}

public void speed_up() {
  if( current_speed < 2 ) return;
  current_speed--;
}

public void speed_down() {
  current_speed++;
}







/* Button Class Taken from Processiong.org Tutorials:
 -> http://processing.org/learning/topics/buttons.html

 And modified for lock parameter, added renamed rect to square button and added real rectbutton
  code cleanup
 */

class Button extends GuiElement
{
  public int highlightcolor;
  boolean over = false;
  //  boolean clicked = false;
  String shortcut = null;
  
  Button(int ix, int iy,  int icolor, int ihighlight) {
    super(ix, iy, icolor);
    this.highlightcolor = ihighlight;
  }

  public boolean clicked() {
    return (this.over && !this.disabled);  
  }

  public boolean key_pressed(int key_code, boolean mac, boolean crtl, boolean alt) {
    return (this.shortcut != null &&  !this.disabled);
  }
  
  public boolean over() {
    return false;
  }

  /* ************************************************************************** */
  protected int current_color() {
    return (this.over && !this.disabled) ? highlightcolor : basecolor;
  }

  protected boolean overRect(int x, int y, int width, int height) {
    return (mouseX >= x && mouseX <= x+width && mouseY >= y && mouseY <= y+height);
  }

  protected boolean overCircle(int x, int y, int diameter) {
    float disX = x - mouseX;
    float disY = y - mouseY;
    return (sqrt(sq(disX) + sq(disY)) < diameter/2 );
  }
}

class CircleButton extends Button {
  int size;

  CircleButton(int ix, int iy, int isize, int icolor, int ihighlight) {
    super( ix, iy, icolor, ihighlight);
    this.size = isize;
  }

  public boolean over() {
    boolean old_over = this.over;
    this.over = overCircle(x, y, size);
    return this.over != old_over;
  }

  public boolean display()  {
    if( !super.display() ) return false;
    ellipse(x, y, size, size);
    return true;
  }
}

class RectButton extends Button {
  int width, height;

  RectButton(int ix, int iy, int iwidth, int iheight, int icolor) {
    this( ix, iy, iwidth, iheight, icolor, icolor);
  }

  RectButton(int ix, int iy, int iwidth, int iheight, int icolor, int ihighlight) {
    super( ix, iy, icolor, ihighlight);
    this.width = iwidth;
    this.height = iheight;
  }

  public boolean over() {
    boolean old_over =  this.over;
    this.over = overRect(x, y, width, height);
    return this.over != old_over;
  }

  public boolean display() {
    if( !super.display() ) return false;
    rect(x, y, width, height);
    return true;
  }
}

class SquareButton extends RectButton {

  SquareButton(int ix, int iy, int isize, int icolor, int ihighlight)  {
    super(ix, iy, isize, isize, icolor, ihighlight);
  }
}

class TextButton extends RectButton {
  String button_text;
  public int text_color;
  float x_offset;
  float y_offset;
  
  TextButton(String itext, int ix, int iy, int iwidth, int iheight, int icolor, int ihighlight) {
    super(ix, iy, iwidth, iheight, icolor, ihighlight);
    this.button_text = itext;
  }

  public boolean display() {
    if( !super.display() ) return false;
    textFont(fontA, 15);
    fill(current_text_color());
    update_offset();
    text(this.current_text(), x_offset, y_offset);
    return true;
  }

  protected int current_text_color() {
    return (this.disabled) ? 0xff666666 : 0xffFFFFFF;
  }
  
  protected String current_text() {
    return this.button_text;
  }
  
  protected void update_offset() {
    x_offset = x + (this.width - textWidth(current_text())) / 2;
    y_offset = y + 17;
  }
  
}

class ActionButton extends TextButton {

  ActionButton(String itext, String ishortcut, int ix, int iy) {
    this(itext, ishortcut, ix, iy, 134, 25, 0xff444444, 0xff999999);
  }

  ActionButton(String itext, String ishortcut, int ix, int iy,  int iwidth, int iheight) {
    this(itext, ishortcut, ix, iy, iwidth, iheight, 0xff444444, 0xff999999);
  }

  ActionButton(String itext, String ishortcut, int ix, int iy, int iwidth, int iheight, int icolor, int ihighlight) {
    super(itext, ix, iy, iwidth, iheight, icolor, ihighlight);
    this.shortcut = ishortcut;
  }

  public boolean clicked() {
    if(!super.clicked()) return false;
    perform_action();
    return true;
  }

  public boolean key_pressed(int key_code, boolean mac, boolean crtl, boolean alt) {
    if(!super.key_pressed(key_code, mac, crtl, alt)) return false;
    String code = "";
    if(mac)  code = "m+" + code;
    if(crtl) code = "c+" + code;
    if(alt) code = "a+" + code;
    if(!this.shortcut.equals(code+PApplet.parseChar(key_code)) && !this.shortcut.equals(code+key_code)) return false; //no shortcut defined
    perform_action();
    return true;
  }

  protected void perform_action() {
    if(this.button_text == "^") matrix.current_frame().shift_up();
    if(this.button_text == "v") matrix.current_frame().shift_down();
    if(this.button_text == "<") matrix.current_frame().shift_left();
    if(this.button_text == ">") matrix.current_frame().shift_right();
    if(this.shortcut    == "m+L") { matrix = matrix.load_from_file(); keyMac  = false;}
    if(this.shortcut    == "m+S") { matrix.save_to_file(); keyMac  = false;}
    if(this.shortcut    == "a+L" && device instanceof StandaloneDevice) matrix = ((StandaloneDevice) device).read_matrix();
    if(this.shortcut    == "a+S" && device instanceof StandaloneDevice) ((StandaloneDevice) device).write_matrix(matrix);
    if(this.button_text == "Add")    matrix.add_frame();
    if(this.button_text == "Delete") matrix.delete_frame();
    if(this.button_text == "Copy")   matrix.copy_frame();
    if(this.button_text == "Paste")  matrix.paste_frame();
    if(this.button_text == "Fill")   matrix.current_frame().fill(matrix.current_color);
    if(this.button_text == "Clear")  matrix.current_frame().clear();
  }
}

class ActionToggleButton extends ActionButton {
  String button_text2;
  boolean locked = false;

  ActionToggleButton(String itext, String itext2, String ishortcut, int ix, int iy)  {
    this(itext, itext2, ishortcut, ix, iy, 134, 25, 0xff444444, 0xff999999);
  }

  ActionToggleButton(String itext, String itext2, String ishortcut, int ix, int iy, int iwidth, int iheight, int icolor, int ihighlight)  {
    super(itext, ishortcut, ix, iy, iwidth, iheight, icolor, ihighlight);
    this.button_text2 = itext2;
  }

  protected String current_text() {
    return this.locked ?  this.button_text2 : this.button_text;
  }

  protected void perform_action() {
     if(this.shortcut == "10") { locked = !locked; toggle_mode(); } // ENTER
     if(this.shortcut == "a+10" && device instanceof StandaloneDevice)  { locked = !locked; ((StandaloneDevice) device).toggle(); } // ENTER
  }
}

class MiniColorButton extends RectButton {

  PixelColor px;
  
  MiniColorButton(int ix, int iy, int iwidth, int iheight, PixelColor icolor) {
    super(ix, iy, iwidth, iheight, icolor.get_color(), icolor.get_color());
    this.px = icolor;    
  }

  public boolean clicked() {
    if(!super.clicked()) return false;
    matrix.current_color.set_color(this.px);
    return true;
  }
  
  public boolean display() {
    if( !super.display() ) return false;
    if(matrix.current_color.equal(this.px)) {
      stroke(0xffFFFF00);
      strokeWeight(2);
      rect(this.x-1, this.y-1, this.width-1, this.height+2);
      strokeWeight(0);
    }
    return true;
  }  
}
public interface Device {

  public void setColorScheme();
  public boolean draw_as_circle();
    
  public boolean enabled();
  public void write_frame(Frame frame);
}

public interface StandaloneDevice {

  public Matrix read_matrix();  
  public void write_matrix(Matrix matrix);
  public void toggle();       
  
  public void speedUp();
  public void speedDown(); 
  
  public void brightnessUp();
  public void brightnessDown();
}
class Frame {

  //static
  PGraphics pg = null;
  int letter_scale = 3;
  PixelColor[] pixs  = null;

  PGraphics frame = null;
  PGraphics thumb = null;

  public int rows = 0;
  public int cols = 0;

  int last_y = 0;
  int last_x = 0;

  Frame(int cols, int rows) {
    this.cols = cols;
    this.rows = rows;
    this.pixs = new PixelColor[rows*cols];
    this.clear();
    this.pg = createGraphics(8*letter_scale, 10*letter_scale, P2D);
  }

  public PGraphics draw_full(int draw_rad, int draw_border) {
    if(this.frame == null) this.frame = draw_canvas(draw_rad, draw_border);
    return this.frame;
  }

  public PGraphics draw_thumb(int draw_rad, int draw_border) {
    if(this.thumb == null) this.thumb = draw_canvas(draw_rad, draw_border);
    return this.thumb;
  }

  public PGraphics draw_canvas(int draw_rad, int draw_border) {
    PGraphics canvas = createGraphics(this.cols * draw_rad, this.rows * draw_rad, P2D);
    canvas.beginDraw();
    canvas.background(55);
    canvas.smooth();
    canvas.noStroke();
    canvas.rectMode(CENTER);
    for(int y = 0; y < this.rows; y++) {
      for(int x = 0; x < this.cols; x++) {
        canvas.fill(this.get_pixel(x,y).get_color());
        if(device.draw_as_circle()) {
          canvas.ellipse( draw_rad * (x + 0.5f), draw_rad * (y + 0.5f), draw_rad-border, draw_rad-border);
        }
        else {  
          canvas.rect( draw_rad * (x + 0.5f), draw_rad * (y + 0.5f), draw_rad-border, draw_rad-border);
        }  
      }
    }
  //  println( "drawn" );
    canvas.endDraw();
    return canvas;
  }

  public void clear() {
    this.set_pixels(new PixelColor());
  }

  public void fill(PixelColor pc) {
    this.set_pixels(pc);
  }
    
  public void set_pixels(PixelColor pc) {
    for( int y = 0; y < this.rows; y++ ) {
      for( int x = 0; x < this.cols; x++ ) {
        set_pixel(x, y, pc);
      }
    }
  }

  public void set_pixels(PixelColor[] pix) {
    for( int y = 0; y < this.rows; y++ ) {
      for( int x = 0; x < this.cols; x++ ) {
        set_pixel(x, y, pix[pos(x,y)]);
      }
    }
  }

  public PixelColor get_pixel(int x, int y) {
    if(x < 0 || y < 0 || x > width || y > height ) return null;
    return pixs[pos(x,y)];
  }

  public PixelColor set_row(int y, PixelColor pc) {
    for( int x = 0; x < this.cols; x++ ) {
      pc = set_colored_pixel(x, y, pc);
    }
    return pc;
  }

  public PixelColor set_col(int x, PixelColor pc) {
    for( int y = 0; y < this.rows; y++ ) {
      pc = set_colored_pixel(x, y, pc);
    }
    return pc;
  }

  public PixelColor set_colored_pixel(int x, int y, PixelColor pc) {
    if( x >= cols ) return set_row( y, pc);
    if( y >= rows)  return set_col( x, pc);
    if( this.get_pixel(x,y).equal(pc) ) {
      this.frame = null;
      this.thumb = null;
      return this.get_pixel(x,y).next_color();
    }
    return set_pixel(x, y, pc);
  }

  public PixelColor set_pixel(int x, int y, PixelColor pc) {
    if( pc == null ) pc = new PixelColor();
    if(this.get_pixel(x,y) != null ) {
      this.get_pixel(x,y).set_color(pc);
    }
    else {
      pixs[pos(x,y)] = pc.clone();
    }
    this.frame = null;
    this.thumb = null;
    return this.get_pixel(x,y).clone();
  }

  public PixelColor update(int x, int y, PixelColor pc, boolean ignore_last) {
    if(!ignore_last && x == last_x && y == last_y) return null;
    last_x = x;
    last_y = y;
    if(color_mode) return set_pixel(x, y, pc);
    return set_colored_pixel(x, y, pc);    
  }

  public Frame clone() {
    Frame f = new Frame(this.cols, this.rows);
    f.set_pixels(pixs);
    return f;
  }

  public void shift_left() {
    for( int y = 0; y < this.rows; y++ ) {
      for( int x = 0; x < this.cols; x++ ) {
        set_pixel(x, y, ( x < this.cols - 1) ? get_pixel(x+1,y) : null );
      }
    }
  }

  public void shift_right() {
    for( int y = 0; y < this.rows; y++ ) {
      for( int x = this.cols - 1; x >= 0; x-- ) {
        set_pixel(x, y, ( x > 0) ? get_pixel(x-1,y) : null );
      }
    }
  }

  public void shift_up() {
    for( int y = 0; y < this.rows; y++ ) {
      for( int x = 0; x < this.cols; x++ ) {
        set_pixel(x, y, ( y < this.rows - 1) ? get_pixel(x,y+1) : null );
      }
    }
  }

  public void shift_down() {
    for( int y = this.rows - 1; y >= 0; y-- ) {
      for( int x = 0; x < this.cols; x++ ) {
        set_pixel(x, y, ( y > 0) ? get_pixel(x,y-1) : null );
      }
    }
  }

  private int pos(int x, int y ) {
    return (y * this.cols) + x;
  }

  private PixelColor set_letter(char letter, PFont font, PixelColor pc) {
    return set_letter(letter, font, pc, 50);
  }

  private PixelColor set_letter(char letter, PFont font, PixelColor pc, int trashhold) {
    PixelColor new_pc = null;
    int offset = 0;
    this.pg.beginDraw();
    this.pg.fill(0xffFF0000);  //red
    this.pg.background(0);
    this.pg.textFont(font,10*letter_scale);
    this.pg.text(letter,0,8*letter_scale);
    this.pg.endDraw();
    this.pg.loadPixels();
    for(int row = 0; row < 10; row++) {
      int sum = 0;
      for(int col = 0; col < 8; col++) {
        if( this.get_pixs(row, col) > trashhold) {
          new_pc = this.set_colored_pixel(col, row-offset, pc);          
          sum++;
        }
      }
      if(sum == 0 && offset < 2) offset++;
      if(row-offset == 7) return new_pc; //exit earlier in case we dont have lower parts
    }
    return new_pc;
  }

  private int get_pixs(int x, int y) {
    int trashhold = 0;
    x *= this.letter_scale;
    y *= this.letter_scale;
    for(int k = 0; k < this.letter_scale; k++) {
      for(int k2 = 0; k2 < this.letter_scale; k2++) {
        trashhold += red(this.pg.pixels[(x+k)*8*this.letter_scale+y+k2]);
      }
    }
    return trashhold / (this.letter_scale * this.letter_scale);
  }
}
class FrameChooser extends RectButton {

  int frame_width;
  int show_frames;
  
  FrameChooser(int ix, int iy, int iwidth, int ishow_frames) {
    super(ix, iy, iwidth * ishow_frames, iwidth, 0xff111111, 0xffFFFF00);
    this.frame_width = iwidth;
    this.show_frames = ishow_frames;
    this.enable();
  }

  public boolean display() {
    if(this.hidden) return false;
    int frame_nr;
    for(int nr = 0; nr < this.show_frames; nr++) {
      frame_nr = this.get_frame_nr(nr);      
      display_frame(this.x + this.frame_width * nr, this.y, frame_nr);
    }
    return true;
  }
  
  private void display_frame(int frame_x, int frame_y, int frame_nr) {
      noFill();
      stroke( (frame_nr == matrix.current_frame_nr) ? this.highlightcolor : this.basecolor );      
      rect(frame_x, this.y, this.frame_width - 10, this.frame_width - 10);    
      if( frame_nr < 0 || frame_nr >= matrix.num_frames()) return;
      image(matrix.frame(frame_nr).draw_thumb(6, 4), frame_x + 1, frame_y + 1);
      fill(255); //white
      noStroke();
      textFont(fontA, 15);
      text(frame_nr + 1, frame_x + 20, frame_y + 62);    
  }
   
  public boolean clicked() {
    if(!super.clicked()) return false;
    int frame_nr =  this.get_frame_nr( (mouseX - this.x) / this.frame_width );
    if( frame_nr >= matrix.num_frames()) return false;
    matrix.current_frame_nr = frame_nr;
    return true;      
  }
  
  public boolean key_pressed(int key_code, boolean mac, boolean crtl, boolean alt) {
     if(this.disabled) return false;
     if(mac || crtl || alt) return false;
     if(key_code == 37) matrix.previous_frame(); // arrow left  //use perform_ation here??
     if(key_code == 39) matrix.next_frame();     // arrow right
     return (key_code == 37 || key_code == 39);
  }

  private int get_frame_nr(int nr) {
    return ( matrix.current_frame_nr > (this.show_frames - 1) ) ? (matrix.current_frame_nr - (this.show_frames - 1) + nr) : nr;
  }
}



class GuiElement {
  int x, y;
  int basecolor;
  boolean hidden = false;
  boolean disabled = false;
  private boolean old_disabled = false;
  
  GuiElement(int ix, int iy, int icolor) {
    this.x = ix;
    this.y = iy;
    this.basecolor = icolor;
  }

  public void disable() {
    old_disabled = disabled;    
    disabled = true;
  }
  
  public void enable() {
    old_disabled = disabled;
    disabled = false;
  }

  public void toggle() {
    disabled = old_disabled;
    hidden = false;
  }

  public void hide() {
    hidden = true;
    disable();
  }

  public void show() {
    hidden = false;
    enable();
  }

  public boolean display() {
    if(this.hidden) return false;
    stroke(30);
    fill(current_color());
    return true;
  }
  
  /* ************************************************************************** */
  protected int current_color() {
    return this.basecolor;
  } 

}

class TextElement extends TextButton {
  
  TextElement( String itext, int ix, int iy ) {
    super(itext, ix, iy, 0, 0, 0xff000000, 0xff000000);
    this.disable();
  }

  protected int current_text_color() {
    return 0xffFFFFFF;
  }

  
  protected void update_offset() {
    x_offset = x;
    y_offset = y + 25;
  }      
}



class LaunchpadDevice implements Device, LaunchpadListener { 

  public Launchpad launchpad;

  private LColor[] buttonColors = new LColor[16];

  boolean colorButtonPressed = false;

  LaunchpadDevice(PApplet app) {
    launchpad = new Launchpad(app);   
    if(enabled()) launchpad.addListener(this);
  }

  public void setColorScheme() {
    PixelColorScheme.R = new int[]{
      0,105,170,255        };   
    PixelColorScheme.G = new int[]{
      0,105,170,255        };   
    PixelColorScheme.B = new int[]{
      0        };   
  }

  public boolean draw_as_circle() {
    return false;
  }

  public boolean enabled() {
    return launchpad.connected();
  }

  /* +++++++++++++++++++++++++++ */
  public void write_frame(Frame frame) {
    LColor[] colors = new LColor[80];
    for( int y = 0; y < frame.rows; y++ ) {
      for( int x = 0; x < frame.cols; x++ ) {
        PixelColor p = frame.get_pixel(x,y);
        colors[y * frame.cols + x] = new LColor(p.r, p.g); 
      }
    }
    if(colorButtonPressed) {
      for( int r = 0; r < 4; r++ ) {
        colors[64 + r] = new LColor(r);
        if(matrix.current_color.r == r ) colors[64 + r].setMode(LColor.BUFFERED);
      }
      for( int g = 0; g < 4; g++ ) {
        colors[68 + g] = new LColor(LColor.RED_OFF, g); 
        if(matrix.current_color.g == g ) colors[68 + g].setMode(LColor.BUFFERED);
      }     
    }

    if( record ) {
      colors[72] = new LColor(LColor.GREEN_LOW);
      colors[73] = new LColor(LColor.GREEN_LOW);     

      colors[76] = new LColor(LColor.RED_LOW);
      colors[77] = new LColor(LColor.RED_LOW);     

      colors[78] = new LColor(matrix.current_color.r, matrix.current_color.g);
      colors[79] = new LColor(LColor.GREEN_LOW); 
    }
    else {
      colors[79] = new LColor(LColor.RED_LOW); 
    } 

    colors[74] = new LColor(LColor.YELLOW_LOW);
    colors[75] = new LColor(LColor.YELLOW_LOW); 

    launchpad.bufferingMode(Launchpad.BUFFER0, Launchpad.BUFFER0);
    launchpad.changeAll(colors);

    if(colorButtonPressed) {
      launchpad.bufferingMode(Launchpad.BUFFER0, Launchpad.BUFFER1, Launchpad.MODE_COPY);
      launchpad.changeSceneButton(LButton.sceneButtonCode(matrix.current_color.r+1), LColor.YELLOW_MEDIUM + LColor.BUFFERED);
      launchpad.changeSceneButton(LButton.sceneButtonCode(matrix.current_color.g+5), LColor.YELLOW_MEDIUM + LColor.BUFFERED);      
    }    
    launchpad.flashingAuto();
  }

  //////////////////////////////    Listener   ////////////////////////////////////
  public void launchpadGridPressed(int x, int y) {
    if(!record) return;
    if(colorButtonPressed) {      
      matrix.current_color = matrix.current_frame().get_pixel(x, y).clone();
    }
    else {
      matrix.click(x * matrix.rad, y * matrix.rad, false);
    }
    mark_for_update();
  }  

  public void launchpadGridReleased(int x, int y) {
  }

  public void launchpadButtonPressed(int buttonCode) {
    if(!record) return;
    if(buttonCode == LButton.USER2) {
      colorButtonPressed = true;
    }
    else {
      launchpad.changeButton(buttonCode, LColor.YELLOW_HIGH);      
    }
    mark_for_update();    
  }  

  public void launchpadButtonReleased(int buttonCode) {
    // launchpad.changeButton(buttonCode, c);
    if( record ) {
      if(buttonCode == LButton.UP)      matrix.add_frame();
      if(buttonCode == LButton.DOWN)    matrix.delete_frame();
      if(buttonCode == LButton.LEFT)    matrix.previous_frame();
      if(buttonCode == LButton.RIGHT)   matrix.next_frame();
      if(buttonCode == LButton.SESSION) matrix.copy_frame();
      if(buttonCode == LButton.USER1)   matrix.paste_frame();
      if(buttonCode == LButton.USER2)   colorButtonPressed = false;          
    }
    else {
      if(buttonCode == LButton.LEFT)    speed_up();
      if(buttonCode == LButton.RIGHT)   speed_down();
    }
    if(buttonCode == LButton.MIXER)   toggle_mode();
    mark_for_update();    
  }

  public void launchpadSceneButtonPressed(int button) {
    int number = LButton.sceneButtonNumber(button);
    if(colorButtonPressed) {
      if( number < 5) { 
        matrix.current_color.r = number - 1;
      } 
      else {
        matrix.current_color.g = number - 5;
      }
    }
    mark_for_update();
  }  

  public void launchpadSceneButtonReleased(int buttonCode) {
  }

}





















class Matrix {

  ArrayList frames  = new ArrayList();

  public int rad = 70;
  int border = 10;

  public int rows = 0;
  public int cols = 0;
  public PixelColor current_color;

  Frame copy_frame;

  int SCALE = 1;

  int current_frame_nr;

  Matrix(int cols, int rows ) {
    this.cols = cols; //X
    this.rows = rows; //Y
    this.current_color = new PixelColor();
    add_frame();
  }

  public int width() {
    return cols * rad;
  }

  public int height() {
    return rows * rad;
  }

  public PGraphics current_frame_image() {
    return this.current_frame_image(rad, border);
  }

  public PGraphics current_frame_image(int draw_rad, int draw_border) {
    return this.current_frame().draw_full(draw_rad, draw_border);
  }

  public boolean click(int x, int y, boolean dragged) {
    if( x < 0 || y < 0) return false; //25 pixel right and bottom for row editing
    if( x > this.width() + 25 || y > this.height() + 25) return false; //25 pixel right and bottom for row editing
    PixelColor pc = this.current_frame().update(x / rad, y / rad, current_color, !dragged);
    if( pc == null ) return false;
    current_color = pc.clone();
    return true;
  }

  public int num_frames() {
    return frames.size();
  }

  public Frame next_frame() {
    current_frame_nr = (current_frame_nr + 1 ) % num_frames();
    return current_frame();
  }

  public Frame previous_frame() {
    current_frame_nr = ( current_frame_nr == 0 ) ? num_frames() - 1 : current_frame_nr - 1;
    return current_frame();
  }

  public Frame first_frame() {
    current_frame_nr = 0;
    return current_frame();
  }

  /* +++++++++++++++ DATA STRUCTURE +++++++++++++++ */
  public Frame current_frame() {
    return frame(current_frame_nr);
  }

  public Frame frame(int f) {
    try {
      return (Frame) frames.get(f);
    }
    catch(Exception e ) {
      return (Frame) frames.get(0);
    }
  }

  public void set_pixel(int f, int x, int y, PixelColor pc) {
    frame(f).set_colored_pixel(x, y, pc);
  }

  /* +++++++++++++++ FRAME +++++++++++++++ */
  public Frame copy_frame() {
    copy_frame = current_frame().clone();
    return current_frame();
  }

  /* +++++++++++++++ FRAME +++++++++++++++ */
  public Frame paste_frame() {
    if( copy_frame != null) frames.set(current_frame_nr, copy_frame.clone()); // better use set_pixel here!?
    return current_frame();
  }

  public Frame add_frame() {
    if(!frames.isEmpty()) current_frame_nr++;
    frames.add(current_frame_nr, new Frame(this.cols, this.rows)); //init first frame
    return current_frame();
  }

  public Frame delete_frame() {
    matrix.copy_frame();
    if(this.num_frames() > 1) {
      frames.remove(current_frame_nr);
      current_frame_nr = current_frame_nr % num_frames();
    }
    return current_frame();
  }

  /* +++++++++++++++ FILE +++++++++++++++ */
  public void save_to_file() {
    String savePath = selectOutput();  // Opens file chooser
    if(savePath == null) {
      println("No output file was selected...");
      return;
    }
    if( match(savePath, ".bmp") == null )  savePath += ".bmp";

    int height = (int) Math.sqrt(this.num_frames());
    while( this.num_frames() % height != 0) {
      height--;
    }
    int width =  this.num_frames() / height;

    PImage output = createImage( width * this.cols, height * this.rows, RGB);

    for(int h = 0; h < height; h++) {
      for(int w = 0; w < width; w++) {
        Frame frame = this.frame(h*width + w);
        for(int y = 0; y < frame.rows; y++) {
          for(int x = 0; x < frame.cols; x++) {
            output.set(x + frame.cols * w, y + frame.rows * h, frame.get_pixel(x,y).get_color() );
          }
        }
      }
    }
    output.save(savePath); //TODO add scaling??
    println("SAVED to " + savePath);
  }

  public Matrix load_from_file() {
    String loadPath = selectInput("Choose a Matrix File to load");  // Opens file chooser
    if(loadPath == null) {
      println("No file was selected...");
      return this;
    }
    if( match(loadPath, ".mtx") != null ) return load_mtx(loadPath); 
    return load_bmp(loadPath);
  }

  public Matrix load_mtx(String loadPath) {
    PixelColor pc;
    Matrix matrix = new Matrix(this.cols, this.rows); //actually we have to read values from File!
    Frame frame = matrix.current_frame();
    BufferedReader reader = createReader(loadPath);
    String line = "";
    while( line != null ) {
      try {
        line = reader.readLine();
        if(line != null && line.length() > 0) {
          String[] str = line.split(",");
          for(int y = 0; y < frame.rows; y++) {
            //invert matrix
            pc = new PixelColor(~Integer.parseInt(str[y*3]), ~Integer.parseInt(str[y*3 + 1]), ~Integer.parseInt(str[y*3 + 2]) );
            frame.set_row(7-y, pc );
          }
          frame = matrix.add_frame();
        }
      }
      catch (IOException e) {
        e.printStackTrace();
        return matrix;
      }
    }
    matrix.delete_frame();
    return matrix;
  }

  public Matrix load_bmp(String loadPath) {
    Matrix matrix = new Matrix(this.cols, this.rows);

    PImage input = loadImage(loadPath);
    input.loadPixels();

    int width = input.width / this.cols / SCALE;
    int height = input.height / this.rows / SCALE;
    Frame frame = matrix.current_frame();
    for(int h = 0; h < height; h++) {
      for(int w = 0; w < width; w++) {
        for(int y = 0; y < frame.rows; y++) {
          for(int x = 0; x < frame.cols; x++) {
            int off = h * width * frame.cols * frame.rows + w * frame.cols + y * (frame.cols * width) + x ;
            int c = input.pixels[off * SCALE];          
            frame.get_pixel(x, y).set_color(c);
          } 
        }
        frame = matrix.add_frame();
      }
    }
    matrix.delete_frame();
    return matrix;
  }
}

static class PixelColorScheme {
  public static int[] R = {};
  public static int[] G = {};
  public static int[] B = {};
}

class PixelColor {
  public int r;
  public int g;
  public int b;

  PixelColor() {
    this(0,0,0);
  }

  PixelColor(int r, int g, int b) {
    set_color(r,g,b);
  }

  public PixelColor next_color() {
    this.set_color_index(this.to_int()+1);
    return this;
  }

  public PixelColor previous_color() {
    this.set_color_index(this.to_int()-1);
    return this;
  }
  
  public int numColors() {
    return PixelColorScheme.R.length * PixelColorScheme.G.length * PixelColorScheme.B.length;
  }


  public boolean equal(PixelColor pc) {
    if(pc == null) return true;
    return this.r == pc.r && this.g == pc.g && this.b == pc.b;
  }

  public void set_color(PixelColor pc) {
    if(pc == null) return;
    this.r = pc.r;
    this.g = pc.g;
    this.b = pc.b;
  }

  public void set_color(int _r, int _g, int _b) {
    this.r = _r;
    this.g = _g;
    this.b = _b;
  }

  public void set_color_index(int i) {
    i = i % numColors();
    if(i < 0) i += numColors();
    this.r = i / (PixelColorScheme.G.length * PixelColorScheme.B.length);
    this.g = (i / PixelColorScheme.B.length) - (this.r * PixelColorScheme.G.length) ;
    this.b = (i - (this.r * PixelColorScheme.G.length + this.g) * PixelColorScheme.B.length);
  }

  public void set_color(int c) {
    int l_index = PixelColorScheme.R.length - 1;
    this.r = PApplet.parseInt(red(c)   / PixelColorScheme.R[l_index] * l_index);

    l_index = PixelColorScheme.G.length - 1;
    this.g = PApplet.parseInt(green(c) / PixelColorScheme.G[l_index] * l_index);

    l_index = PixelColorScheme.B.length - 1;
    this.b = PApplet.parseInt(blue(c)  / PixelColorScheme.B[l_index] * l_index);
  }

  public PixelColor clone() {
    return new PixelColor(r,g,b);
  }

  public int get_color() {
    return color(PixelColorScheme.R[this.r], PixelColorScheme.G[this.g], PixelColorScheme.B[this.b]);
  } 

  public int to_int() {
    return (this.r*(PixelColorScheme.G.length) + this.g)*(PixelColorScheme.B.length) + b; 
  }
}






class RainbowduinoDevice implements Device, StandaloneDevice {

  public Rainbowduino rainbowduino;

  public boolean running;

  int bright = 4;

  RainbowduinoDevice(PApplet app) {
    this(app, null, 0);
  }

  RainbowduinoDevice(PApplet app, String port_name) {
    this(app, port_name, 0);
  }

  RainbowduinoDevice(PApplet app, int baud_rate) {
    this(app, null, baud_rate);
  }

  RainbowduinoDevice(PApplet app, String port_name, int baud_rate) {
    rainbowduino = new Rainbowduino(app);
    rainbowduino.initPort(port_name, baud_rate, true);
    rainbowduino.brightnessSet(this.bright);
    rainbowduino.reset();
    running = false;
  }

  public void setColorScheme() {
    PixelColorScheme.R = new int[]{
      0,255        };   
    PixelColorScheme.G = new int[]{
      0,255        };   
    PixelColorScheme.B = new int[]{
      0,255        };   
  }

  public boolean draw_as_circle() {
    return true;
  }

  public boolean enabled() {
    return rainbowduino.connected();
  }    

  /* +++++++++++++++++++++++++++ */
  public void write_frame(Frame frame) {    
    write_frame(0, frame);
  }

  public void write_frame(int num, Frame frame) {    
    if(frame == null || running || !enabled() ) return;
    rainbowduino.bufferSetAt(num, get_frame_rows(frame));
  }

  public void write_matrix(Matrix matrix) {
    print("Start Writing Matrix - ");
    for(int f = 0; f < matrix.num_frames(); f++) {
      write_frame(f, matrix.frame(f));
    }
    rainbowduino.bufferSave();
    println("Done");
  }

  public Matrix read_matrix() {
    Matrix matrix = new Matrix(8,8);

    print("Start Reading Matrix - ");    
    int frames =  rainbowduino.bufferLoad(); //return num length
    println( "Frames:" + frames);

    for( int frame_nr = 0; frame_nr < frames; frame_nr++ ) {    
      //println("Frame Nr: " + frame_nr);
      Frame frame = matrix.current_frame();
      int frame_byte[] = rainbowduino.bufferGetAt(frame_nr);
      for(int y = 0; y < 8; y++ ) {
        for(int x = 0; x < 8; x++ ) {
          frame.set_pixel(x,y, new PixelColor((frame_byte[3*y+0] >> x) & 1, (frame_byte[3*y+1] >> x) & 1, (frame_byte[3*y+2] >> x) & 1 ) );          
        }
      }      
      matrix.add_frame();
    }           
    matrix.delete_frame();
    println("Done");
    return matrix;
  }

  public void toggle() {
    if(running) {
      running = false;
      rainbowduino.reset();
      return;
    } 
    running = true;
    rainbowduino.bufferLoad();
    rainbowduino.start();   
  }

  public void speedUp() {
    rainbowduino.speedUp();
  }

  public void speedDown() {
    rainbowduino.speedDown();
  }

  public void brightnessUp() {
    this.bright++;
    rainbowduino.brightnessSet(this.bright);
  }

  public void brightnessDown() {
    this.bright--;
    if(this.bright < 1) this.bright = 1;
    rainbowduino.brightnessSet(this.bright);
  }

  ////////////////////////////////////////////////////  
  private int[] get_frame_rows(Frame frame) {
    int[] res = new int[3*frame.rows];
    for( int y = 0; y < frame.rows; y++ ) {
      for( int x = 0; x < frame.cols; x++ ) {
        PixelColor p = frame.get_pixel(x,y);
        res[3*y + 0] |= (p.r & 1) << x;
        res[3*y + 1] |= (p.g & 1) << x;
        res[3*y + 2] |= (p.b & 1) << x;
      }
    }  
    return res;  
  }
}










  static public void main(String args[]) {
    PApplet.main(new String[] { "--bgcolor=#FFFFFF", "mtXcontrol" });
  }
}
