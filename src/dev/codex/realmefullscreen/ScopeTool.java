package dev.codex.realmefullscreen;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
/** Root-only, read-only diagnostics. Never changes LSPosed or module settings. */
public final class ScopeTool {
 public static void main(String[] args){
  try{Class.forName("android.database.sqlite.SQLiteGlobal").getField("sDefaultSyncMode").set(null,"FULL");}catch(Throwable ignored){}
  try(SQLiteDatabase db=SQLiteDatabase.openDatabase("/data/adb/lspd/config/modules_config.db",null,SQLiteDatabase.OPEN_READONLY)){
   try(Cursor c=db.rawQuery("SELECT enabled FROM modules WHERE module_pkg_name=?",new String[]{"dev.codex.realmefullscreen"})){
    System.out.println(c.moveToFirst()?"Module enabled="+c.getInt(0):"Module chưa được LSPosed đăng ký");
   }
   try(Cursor c=db.rawQuery("SELECT app_pkg_name FROM scope JOIN modules USING(mid) WHERE module_pkg_name=? AND user_id=0 ORDER BY app_pkg_name",new String[]{"dev.codex.realmefullscreen"})){
    while(c.moveToNext())System.out.println("SCOPE:"+c.getString(0));
   }
  }catch(Throwable e){System.err.println(e);System.exit(1);}
 }
}
