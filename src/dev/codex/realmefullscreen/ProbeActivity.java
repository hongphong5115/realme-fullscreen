package dev.codex.realmefullscreen;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.*;
import android.view.*;
public final class ProbeActivity extends Activity {
 @Override public void onCreate(Bundle state){super.onCreate(state);
  if(getIntent().hasExtra("rotation")){int r=getIntent().getIntExtra("rotation",0);setRequestedOrientation(r==1?android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:r==3?android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);}
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(Color.rgb(12,102,133));
  TextView bounds=new TextView(this);bounds.setTextColor(Color.WHITE);bounds.setTextSize(18);root.addView(bounds);
  EditText edit=new EditText(this);edit.setSingleLine(true);edit.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI|android.view.inputmethod.EditorInfo.IME_ACTION_DONE);edit.setTextColor(Color.WHITE);edit.setHint("Thử Gboard ngang ở đây");edit.setContentDescription("fullscreen-test-input");root.addView(edit);
  TextView middle=new TextView(this);middle.setText("Nội dung thử độc lập\nXoay 90° / 270°; kiểm tra mép trên và hai cạnh.");middle.setTextColor(Color.WHITE);root.addView(middle,new LinearLayout.LayoutParams(-1,0,1));
  Button bottom=new Button(this);bottom.setText("Nút sát đáy — phải ở trên bàn phím");root.addView(bottom);
  root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{int[] pos=new int[2];root.getLocationOnScreen(pos);bounds.setText("Content x="+pos[0]+" y="+pos[1]+" w="+root.getWidth()+" h="+root.getHeight());});
  setContentView(root);
 }
}
