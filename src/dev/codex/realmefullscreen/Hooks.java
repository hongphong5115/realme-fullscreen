package dev.codex.realmefullscreen;

import android.app.Activity;
import android.app.AndroidAppHelper;
import android.app.KeyguardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Insets;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.util.Set;

public final class Hooks implements IXposedHookLoadPackage {
 static final String TAG="RealmeFullscreen";
 private static final ThreadLocal<Object> layoutWindow=new ThreadLocal<>();
 private static Class<?> stateClass,cutoutClass;
 private static volatile String topPackage="com.android.launcher";
 private static boolean frameworkInstalled,appsInstalled,systemUIInstalled;
 private static Boolean lastUiHidden;
 static void error(String phase,Throwable e){Log.e(TAG,phase,e);XposedBridge.log(TAG+": "+phase+" "+e);}
 static void hook(Class<?> c,String name,XC_MethodHook h){
  Set<XC_MethodHook.Unhook> found=XposedBridge.hookAllMethods(c,name,h);
  XposedBridge.log(TAG+": hook "+c.getName()+"."+name+" count="+found.size());
 }
 static boolean locked(Context c){if(c==null)return true;KeyguardManager k=(KeyguardManager)c.getSystemService(Context.KEYGUARD_SERVICE);return k==null||k.isKeyguardLocked();}
 static boolean windowEligible(Object win){
  if(win==null)return false;
  try{
   WindowManager.LayoutParams a=(WindowManager.LayoutParams)XposedHelpers.getObjectField(win,"mAttrs");
   if(a.type!=WindowManager.LayoutParams.TYPE_INPUT_METHOD&&(a.type<1||a.type>99||a.width!=-1||a.height!=-1))return false;
   if("com.android.systemui".equals(a.packageName))return false;
   Object dc=XposedHelpers.callMethod(win,"getDisplayContent");
   Object policy=XposedHelpers.callMethod(dc,"getDisplayPolicy");
   Config.init((Context)XposedHelpers.getObjectField(policy,"mContext"));
   if(Config.frameworkChanged==null){
    Object service=XposedHelpers.getObjectField(policy,"mService");
    android.os.Handler handler=(android.os.Handler)XposedHelpers.getObjectField(service,"mH");
    Config.frameworkChanged=()->handler.post(()->{try{
     synchronized(XposedHelpers.getObjectField(service,"mGlobalLock")){
      XposedHelpers.callMethod(dc,"setLayoutNeeded");
      XposedHelpers.callMethod(XposedHelpers.callMethod(dc,"getInsetsStateController"),"notifyInsetsChanged");
      XposedHelpers.callMethod(policy,"updateSystemBarAttributes");
      XposedHelpers.callMethod(service,"requestTraversal");
     }
    }catch(Throwable e){error("configuration traversal",e);}});
   }
   if(Boolean.TRUE.equals(XposedHelpers.callMethod(policy,"isKeyguardShowing")))return false;
   int mode=((Number)XposedHelpers.callMethod(win,"getWindowingMode")).intValue();
   return (mode==1||a.type==2011)&&Config.applies(a.packageName);
  }catch(Throwable e){error("window eligibility",e);return false;}
 }
 static Object transformState(Object state,boolean hide,boolean cutout){
  if(state==null||(!hide&&!cutout))return state;
  Object copy=XposedHelpers.newInstance(stateClass,state,true);
  // SDK33 InsetsState uses internal status type 0. Navigation/IME/gesture sources survive.
  if(hide)XposedHelpers.callMethod(copy,"removeSource",0);
  if(cutout){
   Object noCutout=XposedHelpers.getStaticObjectField(cutoutClass,"NO_CUTOUT");
   XposedHelpers.callMethod(copy,"setDisplayCutout",noCutout);
   for(int type=11;type<=14;type++)XposedHelpers.callMethod(copy,"removeSource",type);
  }
  return copy;
 }
 @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp){
  if(Build.VERSION.SDK_INT!=33){XposedBridge.log(TAG+": unsupported SDK; hooks skipped");return;}
  try{
   stateClass=XposedHelpers.findClass("android.view.InsetsState",lp.classLoader);
   cutoutClass=android.view.DisplayCutout.class;
   if(lp.packageName.equals("android")){if(!frameworkInstalled){frameworkInstalled=true;framework(lp.classLoader);}return;}
   if(frameworkInstalled)return;
   if(lp.packageName.equals("com.android.systemui")){if(!systemUIInstalled){systemUIInstalled=true;systemUI(lp.classLoader);}return;}
   if(systemUIInstalled||appsInstalled)return;appsInstalled=true;
   apps(lp);
  }catch(Throwable e){error("setup "+lp.packageName,e);}
 }
 private static void framework(ClassLoader cl){
  Class<?> ws=XposedHelpers.findClass("com.android.server.wm.WindowState",cl);
  hook(ws,"getRequestedVisibility",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{
   if(((Integer)p.args[0])==0&&windowEligible(p.thisObject)){
    WindowManager.LayoutParams a=(WindowManager.LayoutParams)XposedHelpers.getObjectField(p.thisObject,"mAttrs");
    if(a.type!=2011&&Config.hide(a.packageName))p.setResult(false);
   }
  }catch(Throwable e){error("requested visibility",e);}}});
  hook(ws,"getRequestedVisibilities",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{
   if(!windowEligible(p.thisObject))return;
   WindowManager.LayoutParams a=(WindowManager.LayoutParams)XposedHelpers.getObjectField(p.thisObject,"mAttrs");
   if(a.type==2011||!Config.hide(a.packageName))return;
   Object copy=XposedHelpers.newInstance(p.getResult().getClass(),p.getResult());XposedHelpers.callMethod(copy,"setVisibility",0,false);p.setResult(copy);
  }catch(Throwable e){error("requested visibilities",e);}}});
  hook(ws,"getInsetsState",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{
   if(!windowEligible(p.thisObject))return;
   WindowManager.LayoutParams a=(WindowManager.LayoutParams)XposedHelpers.getObjectField(p.thisObject,"mAttrs");
   boolean landscape=isLandscapeState(p.getResult());
   p.setResult(transformState(p.getResult(),Config.hide(a.packageName),(!landscape&&Config.hide(a.packageName))||(landscape&&Config.cutout(a.packageName))));
  }catch(Throwable e){error("window insets",e);}}});
  Class<?> dp=XposedHelpers.findClass("com.android.server.wm.DisplayPolicy",cl);
  hook(dp,"layoutWindowLw",new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){layoutWindow.set(p.args[0]);}
   @Override protected void afterHookedMethod(MethodHookParam p){layoutWindow.remove();}});
  hook(XposedHelpers.findClass("android.view.WindowLayout",cl),"computeFrames",new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){try{
   Object win=layoutWindow.get();if(!windowEligible(win))return;
   WindowManager.LayoutParams a=(WindowManager.LayoutParams)p.args[0];
   boolean landscape=isLandscapeState(p.args[1]);
   boolean hide=Config.hide(a.packageName),cutout=(!landscape&&hide)||(landscape&&Config.cutout(a.packageName));
   WindowManager.LayoutParams copy=new WindowManager.LayoutParams();copy.copyFrom(a);
   if(hide)copy.setFitInsetsTypes(copy.getFitInsetsTypes()&~WindowInsets.Type.statusBars());
   if(cutout)copy.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
   p.args[0]=copy;p.args[1]=transformState(p.args[1],hide,cutout);
  }catch(Throwable e){error("layout frames",e);}}});
 }
 static boolean isLandscapeState(Object state){android.graphics.Rect r=(android.graphics.Rect)XposedHelpers.callMethod(state,"getDisplayFrame");return r.width()>r.height();}
 private static boolean uiHidden(View v){
  try{Config.init(v.getContext());Config.watch(v.getRootView());boolean hidden=!locked(v.getContext())&&Config.hide(topPackage);
   if(lastUiHidden==null||lastUiHidden!=hidden){lastUiHidden=hidden;Log.i(TAG,"SystemUI hidden="+hidden+" top="+topPackage);}return hidden;
  }catch(Throwable e){return false;}
 }
 private static void systemUI(ClassLoader cl){
  Class<?> bar=XposedHelpers.findClass("com.android.systemui.statusbar.phone.PhoneStatusBarView",cl);
  // Suppress rendering only: the native top-edge touch target stays intact.
  XposedHelpers.findAndHookMethod(android.view.ViewGroup.class,"dispatchDraw",Canvas.class,new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){
   if(bar.isInstance(p.thisObject)&&uiHidden((View)p.thisObject))p.setResult(null);
  }});
  XposedHelpers.findAndHookMethod(android.view.ViewGroup.class,"drawChild",Canvas.class,View.class,long.class,new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){
   View child=(View)p.args[1];if(child.getId()==View.NO_ID)return;
   try{
    String id=child.getResources().getResourceEntryName(child.getId());
    if(!(id.equals("quick_qs_status_icons")||id.equals("statusIcons")||id.equals("batteryRemainingIcon")||id.equals("clock")||id.equals("carrier_group")))return;
    if(!uiHidden(child))return;
    android.view.ViewParent parent=child.getParent();
    while(parent instanceof View){String name=parent.getClass().getName();if(name.contains(".qs.")||name.contains("QuickStatusBarHeader")){p.setResult(false);return;}parent=parent.getParent();}
   }catch(android.content.res.Resources.NotFoundException ignored){}
  }});
  Class<?> queue=XposedHelpers.findClass("com.android.systemui.statusbar.CommandQueue",cl);
  hook(queue,"onSystemBarAttributesChanged",new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){
   for(Object arg:p.args)if(arg instanceof String&&!"com.android.systemui".equals(arg)&&!arg.equals(topPackage)){topPackage=(String)arg;Log.i(TAG,"SystemUI controlling package="+topPackage);}
  }});
  XposedBridge.log(TAG+": SystemUI PhoneStatusBarView draw adapter ready");
 }
 private static void apps(XC_LoadPackage.LoadPackageParam lp){
  if(lp.packageName.equals(Config.PKG)){
   XposedHelpers.findAndHookMethod(XposedHelpers.findClass(Config.PKG+".MainActivity",lp.classLoader),"hookActive",XC_MethodReplacement.returnConstant(true));
  }
  Class<?> root=XposedHelpers.findClass("android.view.ViewRootImpl",lp.classLoader);
  hook(root,"getWindowInsets",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{
   Context c=(Context)XposedHelpers.getObjectField(p.thisObject,"mContext");
   Config.init(c);
   Config.watch((View)XposedHelpers.getObjectField(p.thisObject,"mView"));
   WindowManager.LayoutParams a=(WindowManager.LayoutParams)XposedHelpers.getObjectField(p.thisObject,"mWindowAttributes");
   if(locked(c)||!Config.applies(lp.packageName)||!appType(a)||!(p.getResult() instanceof WindowInsets))return;
   Object wc=XposedHelpers.getObjectField(c.getResources().getConfiguration(),"windowConfiguration");
   if(a.type!=2011&&((Number)XposedHelpers.callMethod(wc,"getWindowingMode")).intValue()!=1)return;
   WindowInsets old=(WindowInsets)p.getResult();boolean hide=Config.hide(lp.packageName);
   boolean landscape=c.getResources().getConfiguration().orientation==2;
   boolean cut=(!landscape&&hide)||(landscape&&Config.cutout(lp.packageName));
   WindowInsets.Builder b=new WindowInsets.Builder(old);
   if(hide)b.setInsets(WindowInsets.Type.statusBars(),Insets.NONE).setInsetsIgnoringVisibility(WindowInsets.Type.statusBars(),Insets.NONE).setVisible(WindowInsets.Type.statusBars(),false);
   if(cut)b.setDisplayCutout(null).setInsets(WindowInsets.Type.displayCutout(),Insets.NONE).setInsetsIgnoringVisibility(WindowInsets.Type.displayCutout(),Insets.NONE);
   WindowInsets result=b.build();
   // SDK33 Builder(WindowInsets) resets legacy compatibility fields to systemBars.
   // Preserve them so adjustResize still accounts for IME on legacy Views apps.
   XposedHelpers.setIntField(result,"mCompatInsetsTypes",XposedHelpers.getIntField(old,"mCompatInsetsTypes"));
   XposedHelpers.setBooleanField(result,"mCompatIgnoreVisibility",XposedHelpers.getBooleanField(old,"mCompatIgnoreVisibility"));
   p.setResult(result);
  }catch(Throwable e){error("app insets",e);}}});
  Class<?> layout=XposedHelpers.findClass("android.view.WindowLayout",lp.classLoader);
  hook(layout,"computeFrames",new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){try{
   WindowManager.LayoutParams a=(WindowManager.LayoutParams)p.args[0];
   Context c=AndroidAppHelper.currentApplication();
   Config.init(c);
   if(locked(c)||!Config.applies(lp.packageName)||!appType(a)||((Integer)p.args[4])!=1)return;
   boolean landscape=isLandscapeState(p.args[1]);
   boolean hide=Config.hide(lp.packageName),cut=(!landscape&&hide)||(landscape&&Config.cutout(lp.packageName));
   WindowManager.LayoutParams copy=new WindowManager.LayoutParams();copy.copyFrom(a);
   if(hide)copy.setFitInsetsTypes(copy.getFitInsetsTypes()&~WindowInsets.Type.statusBars());
   if(cut)copy.layoutInDisplayCutoutMode=3;p.args[0]=copy;p.args[1]=transformState(p.args[1],hide,cut);
  }catch(Throwable e){error("client frames",e);}}});
 }
 private static boolean appType(WindowManager.LayoutParams a){return a.type==2011||(a.type>=1&&a.type<=99&&a.width==-1&&a.height==-1);}
}
