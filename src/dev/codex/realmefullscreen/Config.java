package dev.codex.realmefullscreen;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import android.net.Uri;
import android.os.Bundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.WeakHashMap;
import android.view.View;

final class Config {
 static final String PKG="dev.codex.realmefullscreen", PREF="fullscreen";
 static final String PROBE=PKG+".ProbeActivity";
 private static volatile Context context;
 private static final ExecutorService reader=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"Fullscreen-config");t.setDaemon(true);return t;});
 private static long last;
 private static volatile Map<String,?> values=Collections.emptyMap();
 private static final WeakHashMap<View,Boolean> views=new WeakHashMap<>();
 static volatile Runnable frameworkChanged;
 static void watch(View v){if(v!=null)synchronized(views){views.put(v,true);}}
 private static void changed(){
  synchronized(views){for(View v:views.keySet())v.post(()->{v.requestApplyInsets();v.requestLayout();v.invalidate();});}
  Runnable r=frameworkChanged;if(r!=null)r.run();
 }
 static SharedPreferences local(Context c){
  return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
 }
 static void init(Context c){if(c!=null)context=c.getApplicationContext()!=null?c.getApplicationContext():c;}
 static synchronized void refresh(){
  Context c=context;if(c==null)return;
  long now=SystemClock.uptimeMillis();if(now-last<1000)return;last=now;
  // Never perform provider IPC while holding the WindowManager lock or drawing.
  reader.execute(()->{try{
   Bundle result=c.getContentResolver().call(Uri.parse("content://"+PKG+".config"),"read",null,null);
   if(result==null)return;Map<String,Object> fresh=new HashMap<>();
   for(String key:result.keySet())fresh.put(key,result.get(key));
   if(!fresh.equals(values)){values=fresh;android.util.Log.i(Hooks.TAG,"config updated enabled="+fresh.get("enabled")+" hide="+fresh.get("hide")+" cutout="+fresh.get("cutout"));changed();}
  }catch(Throwable e){/* unavailable/locked provider: keep last valid state; default is disabled */}});
 }
 static boolean flag(String key){refresh();return Boolean.TRUE.equals(values.get(key));}
 static boolean applies(String pkg){return pkg!=null&&flag("enabled")&&!Boolean.TRUE.equals(values.get("except."+pkg));}
 static boolean hide(String pkg){return applies(pkg)&&flag("hide");}
 static boolean cutout(String pkg){return applies(pkg)&&flag("cutout");}
}
