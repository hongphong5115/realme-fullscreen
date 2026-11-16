import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.io.FileWriter;

/** Maintenance tool run as root via app_process, never included in the APK. */
public final class LsposedSetup {
 static final String PKG="dev.codex.realmefullscreen",DB="/data/adb/lspd/config/modules_config.db";
 static final String BACKUP="/data/adb/realme_fullscreen_backup_20261113";
 static final String[] SCOPES={"system","com.android.systemui","com.android.launcher","com.google.android.inputmethod.latin","com.android.chrome","com.android.settings","com.facebook.katana","com.google.android.googlequicksearchbox","com.parallelc.micts",PKG};
 public static void main(String[] args)throws Exception{
  if(android.os.Process.myUid()!=0)throw new SecurityException("root required");
  // Oplus assumes every SQLite caller UID owns an installed package; root does not.
  Class.forName("android.database.sqlite.SQLiteGlobal").getField("sDefaultSyncMode").set(null,"FULL");
  try(SQLiteDatabase db=SQLiteDatabase.openDatabase(DB,null,SQLiteDatabase.OPEN_READWRITE)){
   if(args.length>0&&args[0].equals("setup")){
    if(args.length!=2||!args[1].startsWith("/data/app/")||!args[1].endsWith("/base.apk"))throw new IllegalArgumentException("installed APK path required");
    File dir=new File(BACKUP);dir.mkdirs();
    File snapshot=new File(dir,"modules_config.db");
    if(!snapshot.exists())db.execSQL("VACUUM INTO '"+snapshot.getAbsolutePath()+"'");
    db.beginTransaction();try{
     db.execSQL("INSERT OR IGNORE INTO modules(module_pkg_name,apk_path,enabled) VALUES(?,?,1)",new Object[]{PKG,args[1]});
     db.execSQL("UPDATE modules SET apk_path=?,enabled=1 WHERE module_pkg_name=?",new Object[]{args[1],PKG});
     long id;try(Cursor c=db.rawQuery("SELECT mid FROM modules WHERE module_pkg_name=?",new String[]{PKG})){if(!c.moveToFirst())throw new IllegalStateException();id=c.getLong(0);}
     for(String scope:SCOPES)db.execSQL("INSERT OR IGNORE INTO scope(mid,app_pkg_name,user_id) VALUES(?,?,0)",new Object[]{id,scope});
     db.execSQL("DELETE FROM scope WHERE mid=? AND app_pkg_name='android'",new Object[]{id});
     db.execSQL("UPDATE modules SET enabled=0 WHERE module_pkg_name IN ('dev.codex.edge','dev.phong.edgetoedge')");
     db.setTransactionSuccessful();
    }finally{db.endTransaction();}
   }else if(args.length>0&&args[0].equals("rollback")){
    // Restore only old edge-module enabled flags; never replace the live database.
    db.beginTransaction();try{
     db.execSQL("UPDATE modules SET enabled=0 WHERE module_pkg_name=?",new Object[]{PKG});
     try(SQLiteDatabase before=SQLiteDatabase.openDatabase(BACKUP+"/modules_config.db",null,SQLiteDatabase.OPEN_READONLY);
         Cursor c=before.rawQuery("SELECT module_pkg_name,enabled FROM modules WHERE module_pkg_name IN ('dev.codex.edge','dev.phong.edgetoedge')",null)){
      while(c.moveToNext())db.execSQL("UPDATE modules SET enabled=? WHERE module_pkg_name=?",new Object[]{c.getInt(1),c.getString(0)});
     }
     db.setTransactionSuccessful();
    }finally{db.endTransaction();}
   }
   try(Cursor c=db.rawQuery("SELECT module_pkg_name,enabled FROM modules ORDER BY mid",null)){while(c.moveToNext())System.out.println(c.getString(0)+" enabled="+c.getInt(1));}
   try(Cursor c=db.rawQuery("SELECT app_pkg_name FROM scope JOIN modules USING(mid) WHERE module_pkg_name=? AND user_id=0",new String[]{PKG})){while(c.moveToNext())System.out.println("scope="+c.getString(0));}
  }
 }
}
