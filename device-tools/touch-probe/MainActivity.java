package dev.codex.fullscreentouchprobe;
import android.app.Activity;
import android.os.Bundle;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
public final class MainActivity extends Activity {
 int left,right;TextView status;FrameLayout root;
 public void onCreate(Bundle b){super.onCreate(b);int r=getIntent().getIntExtra("rotation",1);setRequestedOrientation(r==3?ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:r==0?ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
  root=new FrameLayout(this);root.setBackgroundColor(Color.rgb(18,70,100));status=new TextView(this);status.setTextSize(17);status.setTextColor(Color.WHITE);status.setGravity(Gravity.CENTER);root.addView(status,new FrameLayout.LayoutParams(-1,160,Gravity.TOP));
  edge(true);edge(false);root.addOnLayoutChangeListener((v,l,t,rr,bb,ol,ot,orr,ob)->update());setContentView(root);root.postDelayed(this::update,1000);
 }
 void edge(boolean isLeft){Button v=new Button(this);v.setText(isLeft?"L":"R");v.setContentDescription(isLeft?"edge-left":"edge-right");FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(80,120,Gravity.TOP|(isLeft?Gravity.LEFT:Gravity.RIGHT));p.topMargin=180;root.addView(v,p);v.setOnClickListener(w->{if(isLeft)left++;else right++;update();});}
 void update(){int[] xy=new int[2];root.getLocationOnScreen(xy);WindowInsets wi=root.getRootWindowInsets();String insets=wi==null?"pending":wi.getInsets(WindowInsets.Type.systemGestures()).toString();status.setText("EDGE L="+left+" R="+right+"\nBounds "+xy[0]+","+xy[1]+" "+root.getWidth()+"x"+root.getHeight()+"\nGestures "+insets);}
}
